package com.invoiceautomationservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceautomationservice.application.model.StoredImageObject;
import com.invoiceautomationservice.application.model.UploadConversationImageCommand;
import com.invoiceautomationservice.application.port.out.ConversationImageRepository;
import com.invoiceautomationservice.application.port.out.ConversationRepository;
import com.invoiceautomationservice.application.port.out.ImageStoragePort;
import com.invoiceautomationservice.application.port.out.MessageRepository;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import com.invoiceautomationservice.domain.model.Conversation;
import com.invoiceautomationservice.domain.model.ConversationChannel;
import com.invoiceautomationservice.domain.model.ConversationImage;
import com.invoiceautomationservice.domain.model.ImageFormat;
import com.invoiceautomationservice.domain.model.ImageRetentionPolicy;
import com.invoiceautomationservice.domain.model.Message;
import com.invoiceautomationservice.infrastructure.config.ImageStorageProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversationImageServiceTest {
  private static final Instant NOW = Instant.parse("2026-10-01T15:00:00Z");
  private ConversationRepository conversations;
  private ConversationImageRepository images;
  private MessageRepository messages;
  private ImageStoragePort storage;
  private CompanyAccessService access;
  private ConversationImageService service;
  private Conversation conversation;

  @BeforeEach
  void setUp() {
    conversations = mock(ConversationRepository.class);
    images = mock(ConversationImageRepository.class);
    messages = mock(MessageRepository.class);
    storage = mock(ImageStoragePort.class);
    access = mock(CompanyAccessService.class);
    ImageStorageProperties properties = new ImageStorageProperties();
    conversation = Conversation.open("company-1", null, null, ConversationChannel.REST,
        "contact", NOW.minusSeconds(60));
    when(conversations.findByIdForUpdate(conversation.id())).thenReturn(conversation);
    when(conversations.findById(conversation.id())).thenReturn(conversation);
    when(conversations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(images.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(storage.store(any(), any(), any())).thenReturn(new StoredImageObject(
        "12/123e4567-e89b-12d3-a456-426614174000.png"));
    service = new ConversationImageService(conversations, images, messages, storage,
        new ImageInspector(properties), properties, access, mock(AuditTrailService.class),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void storesImageMessageAndTemporaryMetadata() throws Exception {
    when(images.findActiveByConversationIdAndSha256(any(), any())).thenReturn(Optional.empty());

    var response = service.upload(conversation.id(), command(png()));

    assertThat(response.contentType()).isEqualTo("image/png");
    assertThat(response.originalFilename()).isEqualTo("ticket.png");
    assertThat(response.width()).isEqualTo(2);
    assertThat(response.height()).isEqualTo(3);
    assertThat(response.expiresAt()).isEqualTo(NOW.plus(30, java.time.temporal.ChronoUnit.DAYS));
    assertThat(response.duplicate()).isFalse();
    verify(storage).store(any(), org.mockito.ArgumentMatchers.eq(ImageFormat.PNG), any());
    verify(messages).save(any(Message.class));
    verify(access).requirePermission("company-1", CompanyPermission.CONVERSATION_MANAGE);
  }

  @Test
  void returnsExistingImageWithoutWritingDuplicateBinary() throws Exception {
    byte[] content = png();
    String hash = new ImageInspector(new ImageStorageProperties())
        .inspect(content, "image/png").sha256();
    ConversationImage existing = image(hash, null);
    when(images.findActiveByConversationIdAndSha256(conversation.id(), hash))
        .thenReturn(Optional.of(existing));

    var response = service.upload(conversation.id(), command(content));

    assertThat(response.id()).isEqualTo(existing.id());
    assertThat(response.duplicate()).isTrue();
    verify(storage, never()).store(any(), any(), any());
    verify(messages, never()).save(any());
  }

  @Test
  void deletesBinaryAndMarksMetadataAsDeleted() {
    ConversationImage image = image("a".repeat(64), null);
    when(images.findByIdAndConversationId(image.id(), conversation.id()))
        .thenReturn(Optional.of(image));

    service.delete(conversation.id(), image.id());

    verify(storage).delete(image.storageKey());
    verify(images).save(org.mockito.ArgumentMatchers.argThat(saved -> !saved.active()));
  }

  @Test
  void removesExpiredTemporaryImages() {
    ConversationImage expired = new ConversationImage(UUID.randomUUID(), conversation.id(),
        UUID.randomUUID(), "12/123e4567-e89b-12d3-a456-426614174000.png", "ticket.png",
        ImageFormat.PNG, 100, 2, 3, "a".repeat(64), ImageRetentionPolicy.TEMPORARY,
        NOW.minusSeconds(1), NOW.minusSeconds(3600), null);
    when(images.findExpired(NOW, 100)).thenReturn(java.util.List.of(expired));

    service.deleteExpired();

    verify(storage).delete(expired.storageKey());
    verify(images).save(org.mockito.ArgumentMatchers.argThat(saved -> !saved.active()));
  }

  private UploadConversationImageCommand command(byte[] content) {
    return new UploadConversationImageCommand("../../ticket.png", "image/png", content,
        "image-1", ImageRetentionPolicy.TEMPORARY, null);
  }

  private ConversationImage image(String hash, Instant deletedAt) {
    UUID id = UUID.randomUUID();
    return new ConversationImage(id, conversation.id(), UUID.randomUUID(),
        "12/123e4567-e89b-12d3-a456-426614174000.png", "ticket.png", ImageFormat.PNG,
        100, 2, 3, hash, ImageRetentionPolicy.TEMPORARY, NOW.plusSeconds(3600), NOW, deletedAt);
  }

  private byte[] png() throws Exception {
    BufferedImage image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }
}
