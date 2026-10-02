package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.CONVERSATION_IMAGE_DELETED;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.CONVERSATION_IMAGE_NOT_FOUND;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_EXTERNAL_MESSAGE_DUPLICATE;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.IMAGE_RETENTION_INVALID;

import com.invoiceautomationservice.application.dto.response.ConversationImageResponse;
import com.invoiceautomationservice.application.model.ConversationImageContent;
import com.invoiceautomationservice.application.model.ImageProcessingRequested;
import com.invoiceautomationservice.application.model.StoredImageObject;
import com.invoiceautomationservice.application.model.UploadConversationImageCommand;
import com.invoiceautomationservice.application.port.in.ConversationImageUseCase;
import com.invoiceautomationservice.application.port.out.ConversationImageRepository;
import com.invoiceautomationservice.application.port.out.ConversationContextRepository;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.application.port.out.ImageStoragePort;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.model.AuditAction;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.ConversationContext;
import com.invoiceautomationservice.domain.model.ConversationImage;
import com.invoiceautomationservice.domain.model.ImageProcessingStatus;
import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
import com.invoiceautomationservice.domain.model.Message;
import com.invoiceautomationservice.domain.model.MessageDirection;
import com.invoiceautomationservice.domain.model.MessageStatus;
import com.invoiceautomationservice.domain.model.MessageType;
import com.invoiceautomationservice.infrastructure.config.ImageStorageProperties;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationImageService implements ConversationImageUseCase {
  private static final int CLEANUP_BATCH_SIZE = 100;

  private final ConversationRepository conversationRepository;
  private final ConversationImageRepository imageRepository;
  private final ConversationContextRepository contextRepository;
  private final MessageRepository messageRepository;
  private final ImageStoragePort storage;
  private final ImageInspector inspector;
  private final ImageStorageProperties properties;
  private final CompanyAccessService accessService;
  private final AuditTrailService auditTrailService;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @Transactional
  public ConversationImageResponse upload(
      UUID conversationId, UploadConversationImageCommand command) {
    var conversation = conversationRepository.findByIdForUpdate(conversationId);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_MANAGE);
    conversation.ensureOpen();
    if (command == null) {
      throw new IllegalArgumentException("image upload command is required");
    }
    InspectedImage inspected = inspector.inspect(command.content(), command.declaredContentType());
    var duplicate = imageRepository.findActiveByConversationIdAndSha256(
        conversationId, inspected.sha256());
    if (duplicate.isPresent()) {
      return toResponse(duplicate.get(), true);
    }
    String externalMessageId = normalizeExternalMessageId(command.externalMessageId());
    if (externalMessageId != null
        && messageRepository.existsByConversationIdAndExternalMessageId(
            conversationId, externalMessageId)) {
      throw new ApplicationException(IMAGE_EXTERNAL_MESSAGE_DUPLICATE,
          externalMessageId, conversationId);
    }
    ImageRetentionPolicy policy = command.retentionPolicy() == null
        ? ImageRetentionPolicy.TEMPORARY : command.retentionPolicy();
    Instant now = Instant.now(clock);
    Instant expiresAt = expiration(policy, command.retentionDays(), now);
    UUID imageId = UUID.randomUUID();
    StoredImageObject stored = storage.store(imageId, inspected.format(), command.content());
    try {
      Message message = messageRepository.save(Message.create(conversationId,
          MessageDirection.INBOUND, MessageType.IMAGE, safeFilename(command.originalFilename(),
              inspected.format().extension()), imageContentUrl(conversationId, imageId),
          externalMessageId, MessageStatus.RECEIVED, now));
      ConversationImage image = imageRepository.save(new ConversationImage(imageId,
          conversationId, message.id(), stored.key(), message.content(), inspected.format(),
          command.content().length, inspected.width(), inspected.height(), inspected.sha256(),
          policy, expiresAt, now, null,
          ImageProcessingStatus.RECEIVED,
          0, null, null, null, null));
      conversationRepository.save(conversation.touch(now));
      auditTrailService.record(conversation.companyId(), AuditAction.IMAGE_STORED,
          "CONVERSATION_IMAGE", image.id(), "SUCCESS",
          inspected.format() + " " + inspected.width() + "x" + inspected.height());
      ConversationContext context = contextRepository.findByConversationId(conversationId)
          .orElseGet(() -> ConversationContext.empty(conversationId, now));
      contextRepository.save(context.markProcessingMedia(now));
      eventPublisher.publishEvent(new ImageProcessingRequested(image.id()));
      return toResponse(image, false);
    } catch (RuntimeException exception) {
      try {
        storage.delete(stored.key());
      } catch (RuntimeException cleanupFailure) {
        exception.addSuppressed(cleanupFailure);
      }
      throw exception;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public ConversationImageResponse findById(UUID conversationId, UUID imageId) {
    var conversation = conversationRepository.findById(conversationId);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_READ);
    return toResponse(findImage(conversationId, imageId), false);
  }

  @Override
  @Transactional(readOnly = true)
  public ConversationImageContent loadContent(UUID conversationId, UUID imageId) {
    var conversation = conversationRepository.findById(conversationId);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_READ);
    ConversationImage image = findImage(conversationId, imageId);
    ensureActive(image);
    return new ConversationImageContent(image.originalFilename(), image.format(),
        storage.load(image.storageKey()));
  }

  @Override
  @Transactional
  public ConversationImageResponse retryProcessing(UUID conversationId, UUID imageId) {
    var conversation = conversationRepository.findByIdForUpdate(conversationId);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_MANAGE);
    ConversationImage image = findImage(conversationId, imageId);
    ensureActive(image);
    ConversationImage queued = image.processingStatus() == ImageProcessingStatus.RECEIVED
        ? image : imageRepository.save(image.retry());
    Instant now = Instant.now(clock);
    ConversationContext context = contextRepository.findByConversationId(conversationId)
        .orElseGet(() -> ConversationContext.empty(conversationId, now));
    contextRepository.save(context.markProcessingMedia(now));
    eventPublisher.publishEvent(new ImageProcessingRequested(image.id()));
    return toResponse(queued, false);
  }

  @Override
  @Transactional
  public void delete(UUID conversationId, UUID imageId) {
    var conversation = conversationRepository.findByIdForUpdate(conversationId);
    accessService.requirePermission(conversation.companyId(), CompanyPermission.CONVERSATION_MANAGE);
    ConversationImage image = findImage(conversationId, imageId);
    ensureActive(image);
    storage.delete(image.storageKey());
    Instant now = Instant.now(clock);
    imageRepository.save(image.delete(now));
    auditTrailService.record(conversation.companyId(), AuditAction.IMAGE_DELETED,
        "CONVERSATION_IMAGE", image.id(), "SUCCESS", "Manual deletion");
  }

  @Scheduled(cron = "${image-storage.cleanup-cron:0 0 3 * * *}")
  @Transactional
  public void deleteExpired() {
    Instant now = Instant.now(clock);
    for (ConversationImage image : imageRepository.findExpired(now, CLEANUP_BATCH_SIZE)) {
      storage.delete(image.storageKey());
      imageRepository.save(image.delete(now));
      String companyId = conversationRepository.findById(image.conversationId()).companyId();
      auditTrailService.recordAs("system", companyId, AuditAction.IMAGE_DELETED,
          "CONVERSATION_IMAGE", image.id(), "SUCCESS", "Retention expired");
    }
  }

  private Instant expiration(
      ImageRetentionPolicy policy, Integer requestedDays, Instant now) {
    if (policy == ImageRetentionPolicy.PERMANENT) {
      if (requestedDays != null) {
        throw new ApplicationException(IMAGE_RETENTION_INVALID,
            properties.getMaxRetentionDays());
      }
      return null;
    }
    int days = requestedDays == null ? properties.getDefaultRetentionDays() : requestedDays;
    if (days < 1 || days > properties.getMaxRetentionDays()) {
      throw new ApplicationException(IMAGE_RETENTION_INVALID,
          properties.getMaxRetentionDays());
    }
    return now.plus(days, ChronoUnit.DAYS);
  }

  private ConversationImage findImage(UUID conversationId, UUID imageId) {
    return imageRepository.findByIdAndConversationId(imageId, conversationId)
        .orElseThrow(() -> new ApplicationException(
            CONVERSATION_IMAGE_NOT_FOUND, imageId, conversationId));
  }

  private void ensureActive(ConversationImage image) {
    if (!image.active()) {
      throw new ApplicationException(CONVERSATION_IMAGE_DELETED, image.id());
    }
  }

  private ConversationImageResponse toResponse(ConversationImage image, boolean duplicate) {
    return new ConversationImageResponse(image.id(), image.conversationId(), image.messageId(),
        image.originalFilename(), image.format().mediaType(), image.sizeBytes(), image.width(),
        image.height(), image.sha256(), image.retentionPolicy(), image.expiresAt(),
        image.createdAt(), image.deletedAt(), duplicate, image.processingStatus(),
        image.processingAttempts(), image.processingStartedAt(), image.processedAt(),
        image.lastProcessingError(), image.interpretation());
  }

  private String imageContentUrl(UUID conversationId, UUID imageId) {
    return "/api/v1/conversations/" + conversationId + "/images/" + imageId + "/content";
  }

  private String safeFilename(String filename, String extension) {
    String value = filename == null ? "image." + extension : filename.replace('\\', '/');
    value = value.substring(value.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").strip();
    if (value.isBlank()) value = "image." + extension;
    return value.length() <= 255 ? value : value.substring(value.length() - 255);
  }

  private String normalizeExternalMessageId(String value) {
    String normalized = value == null || value.isBlank() ? null : value.strip();
    if (normalized != null && normalized.length() > 255) {
      throw new IllegalArgumentException("externalMessageId must not exceed 255 characters");
    }
    return normalized;
  }
}
