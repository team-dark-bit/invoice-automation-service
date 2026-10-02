package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.application.model.ImageProcessingRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ImageProcessingEventListener {
  private final ConversationImageProcessor processor;

  @Async("imageProcessingExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void process(ImageProcessingRequested event) {
    processor.process(event.imageId());
  }
}
