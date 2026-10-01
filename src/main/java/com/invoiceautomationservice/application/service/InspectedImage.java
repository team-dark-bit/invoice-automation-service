package com.invoiceautomationservice.application.service;

import com.invoiceautomationservice.domain.model.ImageFormat;

record InspectedImage(ImageFormat format, int width, int height, String sha256) {}
