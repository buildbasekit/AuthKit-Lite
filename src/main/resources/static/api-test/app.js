"use strict";

const SERVER_CONSOLE_URL = "http://localhost:8080/api-test/index.html";

if (window.location.protocol === "file:") {
  window.addEventListener("DOMContentLoaded", async () => {
    document.getElementById("app").hidden = true;
    document.getElementById("file-launcher").hidden = false;
    const status = document.getElementById("launcher-status");
    try {
      await fetch(SERVER_CONSOLE_URL, { mode: "no-cors", cache: "no-store" });
      status.textContent = "Application found. Redirecting…";
      window.location.replace(SERVER_CONSOLE_URL);
    } catch (_error) {
      status.textContent = "The application is not reachable. Start AuthKit-Lite, then use the button above.";
    }
  });
} else {
  window.addEventListener("DOMContentLoaded", initializeConsole);
}

function initializeConsole() {
  const elements = {
    baseUrl: document.getElementById("base-url"),
    username: document.getElementById("username"),
    email: document.getElementById("email"),
    password: document.getElementById("password"),
    adminUsername: document.getElementById("admin-username"),
    adminPassword: document.getElementById("admin-password"),
    passkeyLabel: document.getElementById("passkey-label"),
    credentialId: document.getElementById("credential-id"),
    results: document.getElementById("results"),
    runnerStatus: document.getElementById("runner-status"),
    warning: document.getElementById("environment-warning"),
    stateUserToken: document.getElementById("state-user-token"),
    stateRefreshToken: document.getElementById("state-refresh-token"),
    stateAdminToken: document.getElementById("state-admin-token"),
    stateCsrf: document.getElementById("state-csrf"),
    statePasskey: document.getElementById("state-passkey")
  };

  const state = {
    userAccessToken: "",
    userRefreshToken: "",
    adminAccessToken: "",
    adminRefreshToken: "",
    csrfToken: "",
    registrationOptions: null,
    authenticationOptions: null,
    running: false
  };

  elements.baseUrl.value = window.location.origin;
  setNewIdentity();
  updateEnvironmentMessage();
  updateState();

  const actions = {
    actuator: () => request("Actuator discovery", "/actuator"),
    health: () => request("Health", "/actuator/health"),
    info: () => request("Info", "/actuator/info"),
    register,
    login,
    refresh,
    logout,
    "admin-login": loginAdmin,
    profile: () => requireUserToken(() => request("Current profile", "/api/users/me", { token: state.userAccessToken })),
    "passkey-list": listPasskeys,
    "admin-users": () => requireAdminToken(() => request("Admin user list", "/api/users?page=0&size=20", { token: state.adminAccessToken })),
    "admin-denied": () => requireUserToken(() => request("Admin endpoint rejects normal user", "/api/users?page=0&size=20", { token: state.userAccessToken, expectedStatuses: [403] })),
    "anonymous-denied": () => request("Protected endpoint rejects anonymous request", "/api/users/me", { expectedStatuses: [401] }),
    csrf: acquireCsrf,
    "registration-options": registrationOptions,
    "passkey-register": registerPasskey,
    "authentication-options": authenticationOptions,
    "passkey-login": loginWithPasskey,
    "passkey-delete": deletePasskey,
    "run-core": runCoreJourney,
    "run-full": runFullJourney,
    reset: resetConsole,
    "clear-results": clearResults
  };

  document.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-action]");
    if (!button || state.running) return;
    const action = actions[button.dataset.action];
    if (!action) return;
    await executeAction(button, action);
  });

  elements.baseUrl.addEventListener("change", () => {
    elements.baseUrl.value = normalizeBaseUrl(elements.baseUrl.value);
    updateEnvironmentMessage();
  });

  async function executeAction(button, action) {
    setRunning(true, button.textContent.trim());
    try {
      await action();
      setRunnerStatus("Ready", "success");
    } catch (error) {
      reportClientError(error);
      setRunnerStatus("Stopped on error", "error");
    } finally {
      setRunning(false);
    }
  }

  function setRunning(running, label = "") {
    state.running = running;
    document.querySelectorAll("button").forEach((button) => { button.disabled = running; });
    if (running) setRunnerStatus(`Running: ${label}`, "running");
  }

  function setRunnerStatus(message, type = "") {
    elements.runnerStatus.textContent = message;
    elements.runnerStatus.className = `runner-status ${type}`.trim();
  }

  function settings() {
    return {
      baseUrl: normalizeBaseUrl(elements.baseUrl.value),
      username: elements.username.value.trim(),
      email: elements.email.value.trim(),
      password: elements.password.value,
      adminUsername: elements.adminUsername.value.trim(),
      adminPassword: elements.adminPassword.value,
      passkeyLabel: elements.passkeyLabel.value.trim() || "Browser test passkey",
      credentialId: elements.credentialId.value.trim()
    };
  }

  function normalizeBaseUrl(value) {
    return value.trim().replace(/\/+$/, "");
  }

  function setNewIdentity() {
    const runId = `${Date.now()}_${Math.floor(Math.random() * 10000)}`;
    elements.username.value = `authkit_web_${runId}`;
    elements.email.value = `authkit_web_${runId}@example.com`;
  }

  function resetConsole() {
    setNewIdentity();
    state.userAccessToken = "";
    state.userRefreshToken = "";
    state.adminAccessToken = "";
    state.adminRefreshToken = "";
    state.csrfToken = "";
    state.registrationOptions = null;
    state.authenticationOptions = null;
    elements.credentialId.value = "";
    updateState();
    clearResults();
  }

  function clearResults() {
    elements.results.innerHTML = '<li class="empty-result">Choose an endpoint above or run a combined journey.</li>';
  }

  function updateEnvironmentMessage() {
    const url = normalizeBaseUrl(elements.baseUrl.value);
    const secure = window.isSecureContext;
    const sameOrigin = url === window.location.origin;
    if (!window.PublicKeyCredential || !navigator.credentials) {
      elements.warning.hidden = false;
      elements.warning.textContent = "This browser does not expose WebAuthn. The core API controls still work, but passkey buttons require a current WebAuthn-capable browser with an available authenticator.";
    } else if (!secure || window.location.hostname !== "localhost") {
      elements.warning.hidden = false;
      elements.warning.textContent = "Passkeys require a trustworthy localhost/HTTPS page whose hostname matches the configured RP ID (localhost). Open http://localhost:8080/api-test/index.html.";
    } else if (!sameOrigin) {
      elements.warning.hidden = false;
      elements.warning.textContent = "This base URL is cross-origin. Add this page origin to authkit.passkey.allowed-origins, or use the same application origin.";
    } else {
      elements.warning.hidden = true;
      elements.warning.textContent = "";
    }
  }

  function updateState() {
    setChip(elements.stateUserToken, "User token", Boolean(state.userAccessToken));
    setChip(elements.stateRefreshToken, "Refresh token", Boolean(state.userRefreshToken));
    setChip(elements.stateAdminToken, "Admin token", Boolean(state.adminAccessToken));
    setChip(elements.stateCsrf, "CSRF/session", Boolean(state.csrfToken));
    setChip(elements.statePasskey, "Passkey", Boolean(elements.credentialId.value.trim()), "selected", "unknown");
  }

  function setChip(element, label, ready, readyWord = "ready", emptyWord = "empty") {
    element.textContent = `${label}: ${ready ? readyWord : emptyWord}`;
    element.classList.toggle("ready", ready);
  }

  async function request(label, path, options = {}) {
    const method = options.method || "GET";
    const headers = new Headers(options.headers || {});
    if (options.token) headers.set("Authorization", `Bearer ${options.token}`);
    if (options.csrfToken) headers.set("X-XSRF-TOKEN", options.csrfToken);
    if (options.body !== undefined) headers.set("Content-Type", "application/json");

    const started = performance.now();
    let response;
    let responseBody;
    try {
      response = await fetch(`${settings().baseUrl}${path}`, {
        method,
        headers,
        credentials: "include",
        cache: "no-store",
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
      });
      responseBody = await readResponse(response);
    } catch (error) {
      addResult({ label, method, path, status: "NETWORK", duration: performance.now() - started, body: { error: error.message }, ok: false });
      throw new Error(`Cannot reach ${settings().baseUrl}. Confirm the application is running and the origin is allowed by CORS.`);
    }

    const expected = options.expectedStatuses || [];
    const ok = response.ok || expected.includes(response.status);
    addResult({ label, method, path, status: response.status, duration: performance.now() - started, body: sanitizeResponse(responseBody), ok });
    if (!ok) throw new ApiError(`${label} failed with HTTP ${response.status}`, response.status, responseBody);
    return responseBody;
  }

  async function readResponse(response) {
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch (_error) { return text; }
  }

  function sanitizeResponse(value) {
    if (!value || typeof value !== "object") return value;
    if (Array.isArray(value)) return value.map(sanitizeResponse);
    return Object.fromEntries(Object.entries(value).map(([key, item]) => {
      if (["accessToken", "refreshToken"].includes(key)) return [key, "<saved in memory; hidden>"];
      return [key, sanitizeResponse(item)];
    }));
  }

  function addResult(result) {
    elements.results.querySelector(".empty-result")?.remove();
    const item = document.createElement("li");
    item.className = `result-item ${result.ok ? "ok" : "error"}`;
    const summary = document.createElement("div");
    summary.className = "result-summary";
    summary.innerHTML = `<span class="result-status"></span><span class="result-method"></span><span class="result-label"></span><span class="result-time"></span>`;
    summary.querySelector(".result-status").textContent = String(result.status);
    summary.querySelector(".result-method").textContent = result.method;
    summary.querySelector(".result-label").textContent = `${result.label} · ${result.path}`;
    summary.querySelector(".result-time").textContent = `${Math.round(result.duration)} ms`;
    const body = document.createElement("pre");
    body.className = "result-body";
    body.textContent = typeof result.body === "string" ? result.body : JSON.stringify(result.body, null, 2);
    item.append(summary, body);
    elements.results.prepend(item);
  }

  function reportClientError(error) {
    addResult({
      label: error.name || "Browser error",
      method: "CLIENT",
      path: window.location.pathname,
      status: "ERROR",
      duration: 0,
      body: { message: error.message },
      ok: false
    });
  }

  function validateRegistrationInput() {
    const value = settings();
    if (value.username.length < 3) throw new Error("Username must contain at least 3 characters.");
    if (!value.email.includes("@")) throw new Error("Enter a valid email address.");
    if (value.password.length < 12) throw new Error("Password must contain at least 12 characters.");
    return value;
  }

  async function register() {
    const value = validateRegistrationInput();
    return request("Register user", "/api/auth/register", {
      method: "POST",
      body: { username: value.username, email: value.email, password: value.password }
    });
  }

  async function login() {
    const value = settings();
    const body = await request("Password login", "/api/auth/login", {
      method: "POST",
      body: { username: value.username, password: value.password }
    });
    saveUserTokens(body);
    return body;
  }

  async function loginAdmin() {
    const value = settings();
    const body = await request("Administrator login", "/api/auth/login", {
      method: "POST",
      body: { username: value.adminUsername, password: value.adminPassword }
    });
    state.adminAccessToken = body.accessToken || "";
    state.adminRefreshToken = body.refreshToken || "";
    updateState();
    return body;
  }

  async function refresh() {
    if (!state.userRefreshToken) throw new Error("Login first so a refresh token is available.");
    const body = await request("Refresh-token rotation", "/api/auth/refresh", {
      method: "POST",
      body: { refreshToken: state.userRefreshToken }
    });
    saveUserTokens(body);
    return body;
  }

  async function logout() {
    if (!state.userRefreshToken || !state.userAccessToken) throw new Error("Login first so user tokens are available.");
    const body = await request("User logout", "/api/auth/logout", {
      method: "POST",
      token: state.userAccessToken,
      body: { refreshToken: state.userRefreshToken }
    });
    state.userAccessToken = "";
    state.userRefreshToken = "";
    updateState();
    return body;
  }

  async function logoutAdmin() {
    if (!state.adminRefreshToken || !state.adminAccessToken) return;
    await request("Administrator logout", "/api/auth/logout", {
      method: "POST",
      token: state.adminAccessToken,
      body: { refreshToken: state.adminRefreshToken }
    });
    state.adminAccessToken = "";
    state.adminRefreshToken = "";
    updateState();
  }

  function saveUserTokens(body) {
    state.userAccessToken = body.accessToken || "";
    state.userRefreshToken = body.refreshToken || "";
    updateState();
  }

  async function requireUserToken(action) {
    if (!state.userAccessToken) throw new Error("Login the normal user first.");
    return action();
  }

  async function requireAdminToken(action) {
    if (!state.adminAccessToken) throw new Error("Login the development administrator first.");
    return action();
  }

  async function listPasskeys() {
    return requireUserToken(async () => {
      const body = await request("List passkeys", "/api/users/me/passkeys", { token: state.userAccessToken });
      if (Array.isArray(body) && body.length > 0) elements.credentialId.value = body[body.length - 1].id;
      updateState();
      return body;
    });
  }

  async function acquireCsrf() {
    const body = await request("Obtain WebAuthn CSRF token", "/webauthn/csrf");
    state.csrfToken = body?.token || "";
    updateState();
    if (!state.csrfToken) throw new Error("The CSRF endpoint did not return a token.");
    return body;
  }

  async function ensureCsrf() {
    if (!state.csrfToken) await acquireCsrf();
  }

  async function registrationOptions() {
    if (!state.userAccessToken) throw new Error("Login the user before requesting registration options.");
    await ensureCsrf();
    state.registrationOptions = await request("Passkey registration options", "/webauthn/register/options", {
      method: "POST",
      token: state.userAccessToken,
      csrfToken: state.csrfToken
    });
    return state.registrationOptions;
  }

  async function registerPasskey() {
    assertWebAuthnAvailable();
    if (!state.registrationOptions) await registrationOptions();
    const publicKey = parseCreationOptions(state.registrationOptions);
    const credential = await navigator.credentials.create({ publicKey });
    if (!credential) throw new Error("The authenticator did not return a credential.");
    const payload = {
      publicKey: {
        credential: credentialToJson(credential),
        label: settings().passkeyLabel
      }
    };
    const body = await request("Complete passkey registration", "/webauthn/register", {
      method: "POST",
      token: state.userAccessToken,
      csrfToken: state.csrfToken,
      body: payload
    });
    state.registrationOptions = null;
    await listPasskeys();
    return body;
  }

  async function authenticationOptions() {
    await ensureCsrf();
    state.authenticationOptions = await request("Passkey authentication options", "/webauthn/authenticate/options", {
      method: "POST",
      csrfToken: state.csrfToken
    });
    return state.authenticationOptions;
  }

  async function loginWithPasskey() {
    assertWebAuthnAvailable();
    if (!state.authenticationOptions) await authenticationOptions();
    const publicKey = parseRequestOptions(state.authenticationOptions);
    const credential = await navigator.credentials.get({ publicKey });
    if (!credential) throw new Error("The authenticator did not return an assertion.");
    const body = await request("Complete passkey authentication", "/login/webauthn", {
      method: "POST",
      csrfToken: state.csrfToken,
      body: credentialToJson(credential)
    });
    saveUserTokens(body);
    state.authenticationOptions = null;
    state.csrfToken = "";
    updateState();
    return body;
  }

  async function deletePasskey() {
    if (!state.userAccessToken) throw new Error("Login as the passkey owner first.");
    const credentialId = settings().credentialId;
    if (!credentialId) throw new Error("List passkeys or paste a credential ID first.");
    await acquireCsrf();
    const body = await request("Delete passkey", `/webauthn/register/${encodeURIComponent(credentialId)}`, {
      method: "DELETE",
      token: state.userAccessToken,
      csrfToken: state.csrfToken
    });
    elements.credentialId.value = "";
    updateState();
    return body;
  }

  function assertWebAuthnAvailable() {
    if (!window.isSecureContext) throw new Error("WebAuthn requires a secure context. Use the localhost page served by AuthKit-Lite.");
    if (!window.PublicKeyCredential || !navigator.credentials) throw new Error("This browser does not support WebAuthn/passkeys.");
  }

  function parseCreationOptions(options) {
    if (typeof PublicKeyCredential.parseCreationOptionsFromJSON === "function") {
      return PublicKeyCredential.parseCreationOptionsFromJSON(options);
    }
    const result = structuredCloneOrJson(options);
    result.challenge = base64UrlToBytes(result.challenge);
    result.user.id = base64UrlToBytes(result.user.id);
    result.excludeCredentials = (result.excludeCredentials || []).map((item) => ({ ...item, id: base64UrlToBytes(item.id) }));
    return result;
  }

  function parseRequestOptions(options) {
    if (typeof PublicKeyCredential.parseRequestOptionsFromJSON === "function") {
      return PublicKeyCredential.parseRequestOptionsFromJSON(options);
    }
    const result = structuredCloneOrJson(options);
    result.challenge = base64UrlToBytes(result.challenge);
    result.allowCredentials = (result.allowCredentials || []).map((item) => ({ ...item, id: base64UrlToBytes(item.id) }));
    return result;
  }

  function credentialToJson(credential) {
    if (typeof credential.toJSON === "function") return credential.toJSON();
    const response = credential.response;
    const json = {
      id: credential.id,
      rawId: bytesToBase64Url(credential.rawId),
      type: credential.type,
      response: {},
      clientExtensionResults: credential.getClientExtensionResults?.() || {},
      authenticatorAttachment: credential.authenticatorAttachment || null
    };
    if (response.attestationObject) {
      json.response.attestationObject = bytesToBase64Url(response.attestationObject);
      json.response.clientDataJSON = bytesToBase64Url(response.clientDataJSON);
      json.response.transports = response.getTransports?.() || [];
    } else {
      json.response.authenticatorData = bytesToBase64Url(response.authenticatorData);
      json.response.clientDataJSON = bytesToBase64Url(response.clientDataJSON);
      json.response.signature = bytesToBase64Url(response.signature);
      json.response.userHandle = response.userHandle ? bytesToBase64Url(response.userHandle) : null;
    }
    return json;
  }

  function base64UrlToBytes(value) {
    const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    const binary = atob(padded);
    return Uint8Array.from(binary, (character) => character.charCodeAt(0));
  }

  function bytesToBase64Url(value) {
    const bytes = new Uint8Array(value);
    let binary = "";
    bytes.forEach((byte) => { binary += String.fromCharCode(byte); });
    return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }

  function structuredCloneOrJson(value) {
    return typeof structuredClone === "function" ? structuredClone(value) : JSON.parse(JSON.stringify(value));
  }

  async function runOperations() {
    await request("Actuator discovery", "/actuator");
    await request("Health", "/actuator/health");
    await request("Info", "/actuator/info");
  }

  async function runCoreJourney() {
    resetConsole();
    await runOperations();
    await register();
    await login();
    await request("Current profile", "/api/users/me", { token: state.userAccessToken });
    await listPasskeys();
    await request("Admin endpoint rejects normal user", "/api/users?page=0&size=20", { token: state.userAccessToken, expectedStatuses: [403] });
    await request("Protected endpoint rejects anonymous request", "/api/users/me", { expectedStatuses: [401] });
    await refresh();
    await loginAdmin();
    await request("Admin user list", "/api/users?page=0&size=20", { token: state.adminAccessToken });
    await logout();
    await logoutAdmin();
  }

  async function runFullJourney() {
    assertWebAuthnAvailable();
    resetConsole();
    await runOperations();
    await register();
    await login();
    await request("Current profile", "/api/users/me", { token: state.userAccessToken });
    await listPasskeys();
    await request("Admin endpoint rejects normal user", "/api/users?page=0&size=20", { token: state.userAccessToken, expectedStatuses: [403] });
    await request("Protected endpoint rejects anonymous request", "/api/users/me", { expectedStatuses: [401] });
    await refresh();
    await loginAdmin();
    await request("Admin user list", "/api/users?page=0&size=20", { token: state.adminAccessToken });
    await acquireCsrf();
    await registrationOptions();
    await registerPasskey();
    await authenticationOptions();
    await loginWithPasskey();
    await request("Profile after passkey login", "/api/users/me", { token: state.userAccessToken });
    await listPasskeys();
    await deletePasskey();
    await logout();
    await logoutAdmin();
  }
}

class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.name = "API error";
    this.status = status;
    this.body = body;
  }
}
