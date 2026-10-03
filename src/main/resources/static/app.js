(() => {
  "use strict";

  const storage = window.sessionStorage;
  const state = {
    token: storage.getItem("invoice.token"),
    username: storage.getItem("invoice.username"),
    companyId: storage.getItem("invoice.companyId"),
    conversationId: storage.getItem("invoice.conversationId"),
    draftId: storage.getItem("invoice.draftId"),
    context: null,
    selectedFile: null,
    previewUrl: null,
    busy: false,
    pollToken: 0
  };

  const $ = (selector) => document.querySelector(selector);
  const loginView = $("#login-view");
  const workspaceView = $("#workspace-view");
  const messages = $("#messages");
  const composer = $("#composer");

  document.addEventListener("DOMContentLoaded", bootstrap);

  async function bootstrap() {
    bindEvents();
    if (!state.token) return showLogin();
    try {
      await api("/api/auth/me", { companyHeader: false });
      await enterWorkspace();
    } catch {
      logout(false);
    }
  }

  function bindEvents() {
    $("#login-form").addEventListener("submit", login);
    $("#logout").addEventListener("click", () => logout(true));
    $("#new-conversation").addEventListener("click", createConversation);
    $("#empty-new-conversation").addEventListener("click", createConversation);
    $("#company-select").addEventListener("change", changeCompany);
    composer.addEventListener("submit", submitComposer);
    $("#message-input").addEventListener("input", autoGrow);
    $("#message-input").addEventListener("keydown", (event) => {
      if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        composer.requestSubmit();
      }
    });
    $("#image-input").addEventListener("change", selectImage);
    $("#quick-summary").addEventListener("click", () => sendText("GENERAR"));
    $("#quick-confirm").addEventListener("click", () => sendText("CONFIRMAR"));
    $("#refresh-draft").addEventListener("click", loadDraft);
    $("#approve-draft").addEventListener("click", approveDraft);
    $("#issue-draft").addEventListener("click", issueDraft);
  }

  async function login(event) {
    event.preventDefault();
    const error = $("#login-error");
    error.textContent = "";
    const button = event.currentTarget.querySelector("button[type=submit]");
    button.disabled = true;
    try {
      const username = $("#username").value.trim();
      const data = await api("/api/auth/login", {
        method: "POST",
        companyHeader: false,
        auth: false,
        body: { username, password: $("#password").value }
      });
      state.token = data.token;
      state.username = data.username || username;
      storage.setItem("invoice.token", state.token);
      storage.setItem("invoice.username", state.username);
      $("#password").value = "";
      await enterWorkspace();
    } catch (exception) {
      error.textContent = exception.message;
    } finally {
      button.disabled = false;
    }
  }

  async function enterWorkspace() {
    loginView.classList.add("hidden");
    workspaceView.classList.remove("hidden");
    const companies = await api("/api/v1/companies", { companyHeader: false });
    renderCompanies(companies);
    if (!companies.length) {
      showToast("El usuario no tiene empresas activas asociadas.", true);
      resetConversation();
      return;
    }
    const validSelection = companies.some((company) => company.id === state.companyId);
    if (!validSelection) setCompany(companies[0].id);
    $("#company-select").value = state.companyId;
    if (state.conversationId) {
      try {
        await restoreConversation();
      } catch {
        resetConversation();
      }
    } else {
      renderConversationAvailability(false);
    }
  }

  function showLogin() {
    workspaceView.classList.add("hidden");
    loginView.classList.remove("hidden");
    setTimeout(() => $("#username").focus(), 0);
  }

  function logout(notify) {
    state.pollToken++;
    Object.assign(state, {
      token: null, username: null, companyId: null, conversationId: null,
      draftId: null, context: null, selectedFile: null
    });
    ["invoice.token", "invoice.username", "invoice.companyId", "invoice.conversationId",
      "invoice.draftId"].forEach((key) => storage.removeItem(key));
    clearPreview();
    messages.replaceChildren();
    showLogin();
    if (notify) showToast("Sesión cerrada.");
  }

  function renderCompanies(companies) {
    const select = $("#company-select");
    select.replaceChildren();
    companies.forEach((company) => {
      const option = document.createElement("option");
      option.value = company.id;
      option.textContent = company.tradeName || company.legalName || company.taxId;
      select.append(option);
    });
  }

  function changeCompany(event) {
    setCompany(event.target.value);
    resetConversation();
    showToast("Empresa activa actualizada.");
  }

  function setCompany(companyId) {
    state.companyId = companyId;
    storage.setItem("invoice.companyId", companyId);
  }

  async function createConversation() {
    if (!state.companyId || state.busy) return;
    setBusy(true);
    try {
      const conversation = await api("/api/v1/conversations", {
        method: "POST",
        body: {
          customerId: null,
          invoiceDraftId: null,
          channel: "REST",
          externalParticipantId: `web-${Date.now()}`
        }
      });
      resetConversation();
      state.conversationId = conversation.id;
      storage.setItem("invoice.conversationId", conversation.id);
      renderConversationAvailability(true);
      appendMessage("assistant",
        "Conversación iniciada. Indica si necesitas una boleta o factura, el DNI/RUC y los productos.");
      $("#message-input").focus();
    } catch (exception) {
      showToast(exception.message, true);
    } finally {
      setBusy(false);
    }
  }

  async function restoreConversation() {
    const conversation = await api(`/api/v1/conversations/${state.conversationId}`);
    if (conversation.companyId !== state.companyId || conversation.status !== "OPEN") {
      throw new Error("La conversación ya no está disponible.");
    }
    renderConversationAvailability(true);
    const page = await api(`/api/v1/conversations/${state.conversationId}/messages?page=0&size=100`);
    messages.replaceChildren();
    page.content.forEach((message) => {
      if (message.type === "TEXT" && message.content) {
        appendMessage(message.direction === "INBOUND" ? "user" : "assistant",
          message.content, message.createdAt);
      }
    });
    if (state.draftId) await loadDraft();
  }

  function resetConversation() {
    state.pollToken++;
    state.conversationId = null;
    state.draftId = null;
    state.context = null;
    storage.removeItem("invoice.conversationId");
    storage.removeItem("invoice.draftId");
    messages.replaceChildren();
    clearPreview();
    renderConversationAvailability(false);
    renderContext(null);
    renderDraft(null);
    $("#issued-card").classList.add("hidden");
  }

  function renderConversationAvailability(active) {
    $("#empty-chat").classList.toggle("hidden", active);
    messages.classList.toggle("hidden", !active);
    composer.classList.toggle("hidden", !active);
    $("#conversation-title").textContent = active
      ? `Conversación ${shortId(state.conversationId)}` : "Sin conversación activa";
    if (!active) $("#flow-state").textContent = "EMPTY";
  }

  async function submitComposer(event) {
    event.preventDefault();
    if (!state.conversationId || state.busy) return;
    const text = $("#message-input").value.trim();
    if (!text && !state.selectedFile) return;
    const file = state.selectedFile;
    setBusy(true);
    try {
      if (file) await uploadImage(file);
      if (text) {
        $("#message-input").value = "";
        autoGrow({ target: $("#message-input") });
        await processText(text);
      }
    } catch (exception) {
      showToast(exception.message, true);
    } finally {
      setBusy(false);
    }
  }

  async function sendText(text) {
    if (!state.conversationId || state.busy) return;
    setBusy(true);
    try {
      await processText(text);
    } catch (exception) {
      showToast(exception.message, true);
    } finally {
      setBusy(false);
    }
  }

  async function processText(text) {
    appendMessage("user", text);
    const response = await api(`/api/v1/conversations/${state.conversationId}/process`, {
      method: "POST",
      body: { text, externalMessageId: uniqueId("web-text") }
    });
    appendMessage("assistant", response.reply);
    state.context = response.context;
    renderContext(response.context);
    if (response.context.invoiceDraftId) {
      state.draftId = response.context.invoiceDraftId;
      storage.setItem("invoice.draftId", state.draftId);
      await loadDraft();
    }
  }

  function selectImage(event) {
    const file = event.target.files?.[0];
    clearPreview();
    if (!file) return;
    state.selectedFile = file;
    state.previewUrl = URL.createObjectURL(file);
    const preview = $("#image-preview");
    const image = document.createElement("img");
    image.src = state.previewUrl;
    image.alt = "Vista previa de la imagen seleccionada";
    const details = document.createElement("div");
    const name = document.createElement("strong");
    name.textContent = file.name;
    const size = document.createElement("small");
    size.textContent = formatBytes(file.size);
    details.append(name, size);
    const remove = document.createElement("button");
    remove.type = "button";
    remove.textContent = "×";
    remove.setAttribute("aria-label", "Quitar imagen");
    remove.addEventListener("click", clearPreview);
    preview.replaceChildren(image, details, remove);
    preview.classList.remove("hidden");
  }

  async function uploadImage(file) {
    const localUrl = state.previewUrl;
    appendImageMessage(file.name, localUrl);
    const body = new FormData();
    body.append("file", file);
    const query = new URLSearchParams({
      externalMessageId: uniqueId("web-image"),
      retentionPolicy: "TEMPORARY"
    });
    clearPreview(false);
    showProcessing(true);
    const image = await api(
      `/api/v1/conversations/${state.conversationId}/images?${query}`, { method: "POST", body });
    renderImageInterpretation(image);
    if (["RECEIVED", "PROCESSING"].includes(image.processingStatus)) {
      pollImage(image.id, ++state.pollToken);
    } else {
      finishImageProcessing(image);
    }
  }

  async function pollImage(imageId, token) {
    for (let attempt = 0; attempt < 40 && token === state.pollToken; attempt++) {
      await delay(1500);
      try {
        const image = await api(
          `/api/v1/conversations/${state.conversationId}/images/${imageId}`);
        renderImageInterpretation(image);
        if (!["RECEIVED", "PROCESSING"].includes(image.processingStatus)) {
          finishImageProcessing(image);
          return;
        }
      } catch (exception) {
        showProcessing(false);
        showToast(exception.message, true);
        return;
      }
    }
    if (token === state.pollToken) {
      showProcessing(false);
      showToast("El procesamiento continúa en segundo plano. Puedes actualizar más tarde.");
    }
  }

  function finishImageProcessing(image) {
    showProcessing(false);
    if (image.processingStatus === "EXTRACTED") {
      appendMessage("assistant",
        "Imagen procesada. Revisa los valores detectados y envía las correcciones necesarias.");
    } else if (image.processingStatus === "FAILED") {
      appendMessage("assistant", "No pude procesar la imagen. Puedes intentar con otra imagen.");
      showToast(image.lastProcessingError || "Falló el procesamiento de la imagen.", true);
    }
  }

  function renderImageInterpretation(image) {
    if (!image?.interpretation) return;
    const interpretation = image.interpretation;
    const detected = {
      documentType: interpretation.documentType,
      recipientDocumentType: interpretation.recipientDocumentType,
      recipientDocumentNumber: interpretation.recipientDocumentNumber,
      currency: interpretation.currency,
      reportedTotal: interpretation.reportedTotal
    };
    (interpretation.items || []).forEach((item, index) => {
      detected[`items[${index}].description`] = item.description;
      detected[`items[${index}].quantity`] = item.quantity;
      detected[`items[${index}].unitPrice`] = item.unitPrice;
    });
    renderReview({
      detectedValues: compactObject(detected),
      confirmedValues: state.context?.review?.confirmedValues || {},
      missingFields: interpretation.missingFields || [],
      ambiguousFields: interpretation.ambiguousFields || [],
      confidence: interpretation.confidence,
      calculationErrors: interpretation.calculationErrors || [],
      confirmationRequired: true
    }, state.context?.state || "PROCESSING_MEDIA");
  }

  function renderContext(context) {
    state.context = context;
    $("#flow-state").textContent = context?.state || "EMPTY";
    if (!context) {
      $("#summary-empty").classList.remove("hidden");
      $("#summary-content").classList.add("hidden");
      $("#confidence-badge").classList.add("hidden");
      return;
    }
    renderReview(context.review, context.state);
  }

  function renderReview(review = {}, flowState) {
    const confirmed = review.confirmedValues || {};
    const detected = review.detectedValues || {};
    const values = { ...detected, ...confirmed };
    const hasValues = Object.keys(values).length > 0;
    $("#summary-empty").classList.toggle("hidden", hasValues);
    $("#summary-content").classList.toggle("hidden", !hasValues);
    $("#flow-state").textContent = flowState || "COLLECTING_DATA";
    if (!hasValues) return;

    const fields = $("#summary-fields");
    fields.replaceChildren();
    addDefinition(fields, "Comprobante", documentTypeLabel(values.documentType));
    const recipient = [values.recipientDocumentType, values.recipientDocumentNumber]
      .filter(Boolean).join(" ");
    addDefinition(fields, "Receptor", recipient || "Pendiente");
    addDefinition(fields, "Moneda", values.currency || "PEN");
    const items = extractItems(values);
    items.forEach((item, index) => addDefinition(fields, `Producto ${index + 1}`,
      `${item.quantity || "?"} × ${item.description || "Pendiente"} · ${money(item.unitPrice, values.currency)}`));
    const calculated = items.reduce((sum, item) => sum
      + (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0), 0);
    if (items.length) addDefinition(fields, "Total calculado", money(calculated, values.currency));

    const confidence = $("#confidence-badge");
    if (review.confidence !== undefined && review.confidence !== null) {
      confidence.textContent = `${Math.round(Number(review.confidence) * 100)}% confianza`;
      confidence.classList.remove("hidden");
    } else {
      confidence.classList.add("hidden");
    }

    const alerts = $("#review-alerts");
    alerts.replaceChildren();
    addAlert(alerts, review.missingFields, "Falta confirmar");
    addAlert(alerts, review.ambiguousFields, "Valor ambiguo");
    addAlert(alerts, review.calculationErrors, "Revisar cálculo", true);
    if (flowState === "AWAITING_DRAFT_CONFIRMATION") {
      addAlert(alerts, ["El resumen está listo. Responde CONFIRMAR para crear el borrador."],
        "Confirmación pendiente");
    }
  }

  async function loadDraft() {
    if (!state.draftId) return;
    try {
      const draft = await api(`/api/v1/invoice-drafts/${state.draftId}`);
      renderDraft(draft);
    } catch (exception) {
      showToast(exception.message, true);
    }
  }

  function renderDraft(draft) {
    $("#draft-empty").classList.toggle("hidden", Boolean(draft));
    $("#draft-content").classList.toggle("hidden", !draft);
    $("#refresh-draft").classList.toggle("hidden", !draft);
    if (!draft) return;
    state.draftId = draft.id;
    storage.setItem("invoice.draftId", draft.id);
    $("#draft-number").textContent = `#${shortId(draft.id)}`;
    $("#draft-status").textContent = draft.status;
    $("#draft-recipient").textContent =
      `${documentTypeLabel(draft.documentType)} · ${draft.recipientDocumentType} ${draft.recipientDocumentNumber}`;
    const list = $("#draft-items");
    list.replaceChildren();
    (draft.items || []).forEach((item) => {
      const row = document.createElement("div");
      row.className = "draft-item";
      const name = document.createElement("strong");
      name.textContent = item.description;
      const total = document.createElement("strong");
      total.textContent = money(item.lineTotal, draft.currency);
      const detail = document.createElement("small");
      detail.textContent = `${formatNumber(item.quantity)} × ${money(item.unitPrice, draft.currency)}`;
      row.append(name, total, detail);
      list.append(row);
    });
    $("#draft-total").textContent = money(draft.total, draft.currency);
    const approve = $("#approve-draft");
    const issue = $("#issue-draft");
    approve.disabled = draft.status !== "DRAFT";
    issue.disabled = draft.status !== "APPROVED";
    approve.textContent = draft.status === "DRAFT" ? "Aprobar" : "Aprobado";
    issue.textContent = draft.status === "ISSUED" ? "Emitido" : "Emitir con mock";
  }

  async function approveDraft() {
    if (!state.draftId || state.busy) return;
    setBusy(true);
    try {
      const draft = await api(`/api/v1/invoice-drafts/${state.draftId}/approve`, { method: "POST" });
      renderDraft(draft);
      showToast("Borrador aprobado. Ya puedes emitirlo con el proveedor mock.");
    } catch (exception) {
      showToast(exception.message, true);
    } finally {
      setBusy(false);
    }
  }

  async function issueDraft() {
    if (!state.draftId || state.busy) return;
    setBusy(true);
    try {
      const document = await api(`/api/v1/invoice-drafts/${state.draftId}/issue`, { method: "POST" });
      renderIssued(document);
      await loadDraft();
      showToast("Comprobante enviado al proveedor mock.");
    } catch (exception) {
      showToast(exception.message, true);
    } finally {
      setBusy(false);
    }
  }

  function renderIssued(document) {
    const card = $("#issued-card");
    card.classList.remove("hidden");
    $("#issued-number").textContent = document.fullNumber || `Documento ${shortId(document.id)}`;
    $("#issued-detail").textContent =
      `${document.status} · ${money(document.total, document.currency)}`;
    card.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    if (options.auth !== false && state.token) headers.set("Authorization", `Bearer ${state.token}`);
    if (options.companyHeader !== false && state.companyId) headers.set("X-Company-Id", state.companyId);
    let body = options.body;
    if (body && !(body instanceof FormData)) {
      headers.set("Content-Type", "application/json");
      body = JSON.stringify(body);
    }
    const response = await fetch(path, { method: options.method || "GET", headers, body });
    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("json") ? await response.json() : null;
    if (!response.ok) {
      if (response.status === 401 && options.auth !== false) logout(false);
      const detail = payload?.errors?.join(" · ") || payload?.message
        || `La solicitud falló con estado ${response.status}.`;
      throw new Error(detail);
    }
    return payload?.data;
  }

  function appendMessage(role, content, timestamp = new Date().toISOString()) {
    const wrapper = document.createElement("article");
    wrapper.className = `message ${role}`;
    const bubble = document.createElement("div");
    bubble.className = "bubble";
    bubble.textContent = content;
    const time = document.createElement("small");
    time.textContent = new Intl.DateTimeFormat("es-PE", {
      hour: "2-digit", minute: "2-digit"
    }).format(new Date(timestamp));
    wrapper.append(bubble, time);
    messages.append(wrapper);
    messages.scrollTop = messages.scrollHeight;
  }

  function appendImageMessage(filename, url) {
    const wrapper = document.createElement("article");
    wrapper.className = "message user";
    if (url) {
      const image = document.createElement("img");
      image.className = "message-image";
      image.src = url;
      image.alt = `Imagen enviada: ${filename}`;
      wrapper.append(image);
    }
    const time = document.createElement("small");
    time.textContent = filename;
    wrapper.append(time);
    messages.append(wrapper);
    messages.scrollTop = messages.scrollHeight;
  }

  function clearPreview(revoke = true) {
    if (revoke && state.previewUrl) URL.revokeObjectURL(state.previewUrl);
    state.selectedFile = null;
    state.previewUrl = null;
    $("#image-input").value = "";
    $("#image-preview").replaceChildren();
    $("#image-preview").classList.add("hidden");
  }

  function showProcessing(show) {
    $("#processing-banner").classList.toggle("hidden", !show);
  }

  function setBusy(busy) {
    state.busy = busy;
    $("#send-message").disabled = busy;
    $("#new-conversation").disabled = busy;
  }

  function addDefinition(list, term, value) {
    if (!value) return;
    const dt = document.createElement("dt");
    const dd = document.createElement("dd");
    dt.textContent = term;
    dd.textContent = value;
    list.append(dt, dd);
  }

  function addAlert(container, values, title, error = false) {
    if (!values?.length) return;
    const alert = document.createElement("div");
    alert.className = `review-alert${error ? " error" : ""}`;
    alert.textContent = `${title}: ${values.join(", ")}`;
    container.append(alert);
  }

  function extractItems(values) {
    const indexes = new Set();
    Object.keys(values).forEach((key) => {
      const match = key.match(/^items\[(\d+)]\./);
      if (match) indexes.add(Number(match[1]));
    });
    return [...indexes].sort((a, b) => a - b).map((index) => ({
      description: values[`items[${index}].description`],
      quantity: values[`items[${index}].quantity`],
      unitPrice: values[`items[${index}].unitPrice`]
    }));
  }

  function autoGrow(event) {
    const field = event.target;
    field.style.height = "auto";
    field.style.height = `${Math.min(field.scrollHeight, 160)}px`;
  }

  function showToast(message, error = false) {
    const toast = document.createElement("div");
    toast.className = `toast${error ? " error" : ""}`;
    toast.textContent = message;
    $("#toast-region").append(toast);
    setTimeout(() => toast.remove(), 4500);
  }

  function documentTypeLabel(type) {
    return ({ SALES_RECEIPT: "Boleta", INVOICE: "Factura",
      CREDIT_NOTE: "Nota de crédito", DEBIT_NOTE: "Nota de débito" })[type] || type;
  }

  function money(value, currency = "PEN") {
    if (value === undefined || value === null || value === "") return "Pendiente";
    const number = Number(value);
    if (!Number.isFinite(number)) return String(value);
    return new Intl.NumberFormat("es-PE", {
      style: "currency", currency: currency || "PEN", minimumFractionDigits: 2
    }).format(number);
  }

  function formatNumber(value) {
    return Number(value).toLocaleString("es-PE", { maximumFractionDigits: 4 });
  }

  function formatBytes(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  function compactObject(value) {
    return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== null && item !== undefined));
  }

  function shortId(value) {
    return value ? String(value).slice(0, 8) : "";
  }

  function uniqueId(prefix) {
    const id = window.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    return `${prefix}-${id}`;
  }

  function delay(milliseconds) {
    return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
  }
})();
