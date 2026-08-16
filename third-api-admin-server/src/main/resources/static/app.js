(function () {
    "use strict";

    const state = {
        view: "apps",
        apps: [],
        providers: [],
        channels: [],
        endpoints: [],
        authConfig: null,
        healthChecks: [],
        auditLogs: [],
        selectedChannelId: null,
        selectedAuthChannelId: null,
        selectedHealthChannelId: null,
        modal: null
    };

    const viewMeta = {
        apps: { title: "应用管理", create: "新增应用", render: renderApps },
        providers: { title: "服务商管理", create: "新增服务商", render: renderProviders },
        channels: { title: "渠道管理", create: "新增渠道", render: renderChannels },
        endpoints: { title: "接口管理", create: "新增接口", render: renderEndpoints },
        auth: { title: "鉴权配置", create: "保存鉴权", render: renderAuth },
        health: { title: "健康监测", create: "运行检查", render: renderHealth },
        audit: { title: "审计日志", create: "刷新", render: renderAudit }
    };

    document.addEventListener("DOMContentLoaded", init);

    function init() {
        document.querySelectorAll(".nav-item[data-view]").forEach(function (button) {
            button.addEventListener("click", function () {
                const view = button.dataset.view;
                if (view === "refresh") {
                    loadAll();
                    return;
                }
                switchView(view);
            });
        });

        document.getElementById("refreshBtn").addEventListener("click", loadAll);
        document.getElementById("createBtn").addEventListener("click", openCreate);
        document.getElementById("modalClose").addEventListener("click", closeModal);
        document.getElementById("modalCancel").addEventListener("click", closeModal);
        document.getElementById("modalSave").addEventListener("click", function () {
            if (state.modal && state.modal.onSave) {
                state.modal.onSave();
            }
        });
        document.getElementById("channelSelect").addEventListener("change", function () {
            state.selectedChannelId = this.value ? Number(this.value) : null;
            loadEndpoints();
        });
        document.getElementById("authChannelSelect").addEventListener("change", function () {
            state.selectedAuthChannelId = this.value ? Number(this.value) : null;
            loadAuthConfig();
        });
        document.getElementById("healthChannelSelect").addEventListener("change", function () {
            state.selectedHealthChannelId = this.value ? Number(this.value) : null;
            loadHealthChecks();
        });
        document.getElementById("saveAuthBtn").addEventListener("click", saveAuthConfig);
        document.getElementById("runHealthBtn").addEventListener("click", runHealthCheck);

        loadAll();
    }

    function switchView(view) {
        state.view = view;
        document.querySelectorAll(".nav-item[data-view]").forEach(function (button) {
            button.classList.toggle("active", button.dataset.view === view);
        });
        document.querySelectorAll(".view").forEach(function (section) {
            section.hidden = section.id !== "view-" + view;
        });
        document.getElementById("viewTitle").textContent = viewMeta[view].title;
        document.getElementById("pageTitle").textContent = viewMeta[view].title;
        document.getElementById("createBtnText").textContent = viewMeta[view].create;
        document.getElementById("createBtn").hidden = ["auth", "health", "audit"].indexOf(view) !== -1;
        if (view === "endpoints") {
            fillChannelSelect();
        } else if (view === "auth") {
            fillAuthSelect();
            loadAuthConfig();
        } else if (view === "health") {
            fillHealthSelect();
            loadHealthChecks();
        } else if (view === "audit") {
            loadAuditLogs();
        }
        refreshIcons();
        viewMeta[view].render();
    }

    async function loadAll() {
        try {
            const results = await Promise.all([loadApps(), loadProviders(), loadChannels()]);
            state.apps = results[0];
            state.providers = results[1];
            state.channels = results[2];
            if (state.view === "endpoints") {
                await loadEndpoints();
            }
            switchView(state.view);
            showToast("数据已刷新");
        } catch (error) {
            showToast(error.message, "err");
        }
    }

    async function loadApps() {
        state.apps = await api("GET", "/api/v1/admin/apps");
    }

    async function loadProviders() {
        state.providers = await api("GET", "/api/v1/admin/providers");
    }

    async function loadChannels() {
        state.channels = await api("GET", "/api/v1/admin/channels");
    }

    async function loadEndpoints() {
        if (!state.selectedChannelId) {
            state.endpoints = [];
            renderEndpoints();
            return;
        }
        state.endpoints = await api("GET", "/api/v1/admin/channels/" + state.selectedChannelId + "/endpoints");
        renderEndpoints();
    }

    function renderApps() {
        document.getElementById("appsCount").textContent = state.apps.length + " 个应用";
        const body = document.getElementById("appsBody");
        body.innerHTML = state.apps.map(function (app) {
            return "<tr>"
                + "<td class=\"mono\">" + esc(app.id) + "</td>"
                + "<td class=\"mono\">" + esc(app.appId) + "</td>"
                + "<td>" + esc(app.appName) + "</td>"
                + "<td class=\"mono\">" + maskSecret(app.appSecret) + "</td>"
                + "<td><span class=\"status\"><span class=\"dot " + (app.enabled ? "on" : "off") + "\"></span>"
                + (app.enabled ? "启用" : "停用") + "</span></td>"
                + "<td><div class=\"row-actions\">"
                + actionButton("edit", "编辑", app.id)
                + actionButton("bind", "绑定", app.id)
                + actionButton("publish", "发布", app.id)
                + actionButton("delete", "删除", app.id)
                + "</div></td></tr>";
        }).join("");
        attachActions("appsBody", {
            edit: function (id) { openAppModal(id); },
            bind: function (id) { openBindModal(id); },
            publish: function (id) { publishApp(id); },
            delete: function (id) { deleteApp(id); }
        });
        refreshIcons();
    }

    function renderProviders() {
        document.getElementById("providersCount").textContent = state.providers.length + " 个服务商";
        const body = document.getElementById("providersBody");
        body.innerHTML = state.providers.map(function (provider) {
            return "<tr>"
                + "<td class=\"mono\">" + esc(provider.id) + "</td>"
                + "<td class=\"mono\">" + esc(provider.code) + "</td>"
                + "<td>" + esc(provider.name) + "</td>"
                + "<td>" + esc(provider.category || "OTHER") + "</td>"
                + "<td><span class=\"status\"><span class=\"dot " + (provider.enabled ? "on" : "off") + "\"></span>"
                + (provider.enabled ? "启用" : "停用") + "</span></td>"
                + "<td><div class=\"row-actions\">"
                + actionButton("edit", "编辑", provider.id)
                + actionButton("delete", "删除", provider.id)
                + "</div></td></tr>";
        }).join("");
        attachActions("providersBody", {
            edit: function (id) { openProviderModal(id); },
            delete: function (id) { deleteProvider(id); }
        });
        refreshIcons();
    }

    function renderChannels() {
        document.getElementById("channelsCount").textContent = state.channels.length + " 个渠道";
        const body = document.getElementById("channelsBody");
        body.innerHTML = state.channels.map(function (channel) {
            return "<tr>"
                + "<td class=\"mono\">" + esc(channel.id) + "</td>"
                + "<td>" + esc(channel.providerCode || channel.providerId) + "</td>"
                + "<td class=\"mono\">" + esc(channel.code) + "</td>"
                + "<td>" + esc(channel.name) + "</td>"
                + "<td class=\"mono\">" + esc(channel.baseUrl) + "</td>"
                + "<td>" + esc(channel.environment) + "</td>"
                + "<td><span class=\"status\"><span class=\"dot " + (channel.enabled ? "on" : "off") + "\"></span>"
                + (channel.enabled ? "启用" : "停用") + "</span></td>"
                + "<td><div class=\"row-actions\">"
                + actionButton("edit", "编辑", channel.id)
                + actionButton("endpoints", "接口", channel.id)
                + actionButton("delete", "删除", channel.id)
                + "</div></td></tr>";
        }).join("");
        attachActions("channelsBody", {
            edit: function (id) { openChannelModal(id); },
            endpoints: function (id) { goToChannelEndpoints(id); },
            delete: function (id) { deleteChannel(id); }
        });
        refreshIcons();
    }

    function renderEndpoints() {
        const body = document.getElementById("endpointsBody");
        if (!state.selectedChannelId) {
            body.innerHTML = "<tr><td colspan=\"9\" class=\"muted\">请先选择渠道</td></tr>";
            return;
        }
        body.innerHTML = state.endpoints.map(function (endpoint) {
            return "<tr>"
                + "<td class=\"mono\">" + esc(endpoint.id) + "</td>"
                + "<td class=\"mono\">" + esc(endpoint.code) + "</td>"
                + "<td>" + esc(endpoint.name) + "</td>"
                + "<td><span class=\"mono\">" + esc(endpoint.httpMethod) + "</span></td>"
                + "<td class=\"mono\">" + esc(endpoint.path) + "</td>"
                + "<td>" + esc(endpoint.timeoutMs) + "ms</td>"
                + "<td>" + esc(endpoint.retryMax) + "</td>"
                + "<td><span class=\"status\"><span class=\"dot " + (endpoint.enabled ? "on" : "off") + "\"></span>"
                + (endpoint.enabled ? "启用" : "停用") + "</span></td>"
                + "<td><div class=\"row-actions\">"
                + actionButton("edit", "编辑", endpoint.id)
                + actionButton("delete", "删除", endpoint.id)
                + "</div></td></tr>";
        }).join("");
        attachActions("endpointsBody", {
            edit: function (id) { openEndpointModal(id); },
            delete: function (id) { deleteEndpoint(id); }
        });
        refreshIcons();
    }

    function fillChannelSelect() {
        const select = document.getElementById("channelSelect");
        select.innerHTML = channelOptionsHtml(state.selectedChannelId);
        if (!state.selectedChannelId && state.channels.length > 0) {
            state.selectedChannelId = state.channels[0].id;
            select.value = String(state.selectedChannelId);
            loadEndpoints();
        }
    }

    function goToChannelEndpoints(channelId) {
        state.selectedChannelId = channelId;
        switchView("endpoints");
        fillChannelSelect();
        loadEndpoints();
    }

    function fillAuthSelect() {
        const select = document.getElementById("authChannelSelect");
        select.innerHTML = channelOptionsHtml(state.selectedAuthChannelId);
        if (!state.selectedAuthChannelId && state.channels.length > 0) {
            state.selectedAuthChannelId = state.channels[0].id;
            select.value = String(state.selectedAuthChannelId);
        }
    }

    function fillHealthSelect() {
        const select = document.getElementById("healthChannelSelect");
        select.innerHTML = channelOptionsHtml(state.selectedHealthChannelId);
        if (!state.selectedHealthChannelId && state.channels.length > 0) {
            state.selectedHealthChannelId = state.channels[0].id;
            select.value = String(state.selectedHealthChannelId);
        }
    }

    function channelOptionsHtml(selectedId) {
        return "<option value=\"\">选择渠道</option>"
            + state.channels.map(function (channel) {
                return "<option value=\"" + esc(channel.id) + "\""
                    + (selectedId === channel.id ? " selected" : "") + ">"
                    + esc(channel.name) + " · " + esc(channel.code) + "</option>";
            }).join("");
    }

    function loadAuthConfig() {
        if (!state.selectedAuthChannelId) {
            state.authConfig = null;
            renderAuth();
            return;
        }
        api("GET", "/api/v1/admin/channels/" + state.selectedAuthChannelId + "/auth")
            .then(function (config) {
                state.authConfig = config || {};
                renderAuth();
            })
            .catch(function (error) {
                showToast(error.message, "err");
            });
    }

    function saveAuthConfig() {
        if (!state.selectedAuthChannelId) {
            showToast("请先选择渠道", "err");
            return;
        }
        const body = {
            authType: document.getElementById("authType").value,
            tokenUrl: document.getElementById("authTokenUrl").value,
            clientId: document.getElementById("authClientId").value,
            clientSecret: document.getElementById("authClientSecret").value,
            tokenCacheTtlSeconds: Number(document.getElementById("authTtl").value || 300),
            enabled: document.getElementById("authEnabled").value === "true"
        };
        api("PUT", "/api/v1/admin/channels/" + state.selectedAuthChannelId + "/auth", body)
            .then(function () {
                showToast("鉴权配置已保存");
                loadAuthConfig();
            })
            .catch(function (error) {
                showToast(error.message, "err");
            });
    }

    function renderAuth() {
        const config = state.authConfig || {};
        document.getElementById("authType").value = config.authType || "NONE";
        document.getElementById("authTokenUrl").value = config.tokenUrl || "";
        document.getElementById("authClientId").value = config.clientId || "";
        document.getElementById("authClientSecret").value = config.clientSecret || "";
        document.getElementById("authTtl").value = config.tokenCacheTtlSeconds || 300;
        document.getElementById("authEnabled").value = String(config.enabled !== false);
        refreshIcons();
    }

    function loadHealthChecks() {
        if (!state.selectedHealthChannelId) {
            state.healthChecks = [];
            renderHealth();
            return;
        }
        api("GET", "/api/v1/admin/health-checks?channelId=" + state.selectedHealthChannelId)
            .then(function (rows) {
                state.healthChecks = rows || [];
                renderHealth();
            })
            .catch(function (error) {
                showToast(error.message, "err");
            });
    }

    function runHealthCheck() {
        if (!state.selectedHealthChannelId) {
            showToast("请先选择渠道", "err");
            return;
        }
        api("POST", "/api/v1/admin/health-checks/run?channelId=" + state.selectedHealthChannelId)
            .then(function () {
                showToast("检查完成");
                loadHealthChecks();
            })
            .catch(function (error) {
                showToast(error.message, "err");
            });
    }

    function renderHealth() {
        const body = document.getElementById("healthBody");
        if (!state.selectedHealthChannelId) {
            body.innerHTML = "<tr><td colspan=\"7\" class=\"muted\">请先选择渠道</td></tr>";
            return;
        }
        body.innerHTML = state.healthChecks.map(function (check) {
            const success = check.success;
            return "<tr>"
                + "<td class=\"mono\">" + esc(check.id) + "</td>"
                + "<td>" + esc(check.channelId) + "</td>"
                + "<td class=\"mono\">" + esc(check.endpointId || "—") + "</td>"
                + "<td class=\"mono\">" + esc(check.targetUrl) + "</td>"
                + "<td><span class=\"status\"><span class=\"dot " + (success ? "on" : "off") + "\"></span>"
                + (success ? "UP" : "DOWN") + "</span></td>"
                + "<td>" + esc(check.costMs) + "ms</td>"
                + "<td class=\"mono\">" + esc(check.checkedAt) + "</td></tr>";
        }).join("") || "<tr><td colspan=\"7\" class=\"muted\">暂无检查记录</td></tr>";
        refreshIcons();
    }

    function loadAuditLogs() {
        api("GET", "/api/v1/admin/audit-logs")
            .then(function (rows) {
                state.auditLogs = rows || [];
                renderAudit();
            })
            .catch(function (error) {
                showToast(error.message, "err");
            });
    }

    function renderAudit() {
        document.getElementById("auditCount").textContent = state.auditLogs.length + " 条";
        const body = document.getElementById("auditBody");
        body.innerHTML = state.auditLogs.map(function (log) {
            return "<tr>"
                + "<td class=\"mono\">" + esc(log.id) + "</td>"
                + "<td>" + esc(log.operator) + "</td>"
                + "<td><span class=\"mono\">" + esc(log.action) + "</span></td>"
                + "<td>" + esc(log.targetType) + "</td>"
                + "<td class=\"mono\">" + esc(log.targetId) + "</td>"
                + "<td class=\"mono\">" + esc(shortJson(log.afterJson)) + "</td>"
                + "<td class=\"mono\">" + esc(log.createdAt) + "</td></tr>";
        }).join("") || "<tr><td colspan=\"7\" class=\"muted\">暂无审计日志</td></tr>";
        refreshIcons();
    }

    function shortJson(value) {
        if (!value) {
            return "—";
        }
        const text = typeof value === "string" ? value : JSON.stringify(value);
        return text.length > 60 ? text.slice(0, 60) + "…" : text;
    }

    function openCreate() {
        if (state.view === "apps") {
            openAppModal();
        } else if (state.view === "providers") {
            openProviderModal();
        } else if (state.view === "channels") {
            openChannelModal();
        } else if (state.view === "endpoints") {
            openEndpointModal();
        }
    }

    function openAppModal(id) {
        const app = id ? state.apps.find(function (item) { return item.id === id; }) : null;
        openModal(id ? "编辑应用" : "新增应用", appForm(app), function () {
            const body = readForm("appForm");
            const request = id ? api("PUT", "/api/v1/admin/apps/" + id, body) : api("POST", "/api/v1/admin/apps", body);
            request.then(function () { closeModal(); loadAll(); });
        });
    }

    function openProviderModal(id) {
        const provider = id ? state.providers.find(function (item) { return item.id === id; }) : null;
        openModal(id ? "编辑服务商" : "新增服务商", providerForm(provider), function () {
            const body = readForm("providerForm");
            const request = id ? api("PUT", "/api/v1/admin/providers/" + id, body) : api("POST", "/api/v1/admin/providers", body);
            request.then(function () { closeModal(); loadAll(); });
        });
    }

    function openChannelModal(id) {
        const channel = id ? state.channels.find(function (item) { return item.id === id; }) : null;
        openModal(id ? "编辑渠道" : "新增渠道", channelForm(channel), function () {
            const body = readForm("channelForm");
            const request = id ? api("PUT", "/api/v1/admin/channels/" + id, body) : api("POST", "/api/v1/admin/channels", body);
            request.then(function () { closeModal(); loadAll(); });
        });
    }

    function openEndpointModal(id) {
        if (!state.selectedChannelId) {
            showToast("请先选择渠道", "err");
            return;
        }
        const endpoint = id ? state.endpoints.find(function (item) { return item.id === id; }) : null;
        openModal(id ? "编辑接口" : "新增接口", endpointForm(endpoint), function () {
            const body = readForm("endpointForm");
            const url = id
                ? "/api/v1/admin/endpoints/" + id
                : "/api/v1/admin/channels/" + state.selectedChannelId + "/endpoints";
            const request = id ? api("PUT", url, body) : api("POST", url, body);
            request.then(function () { closeModal(); loadEndpoints(); });
        });
    }

    function openBindModal(id) {
        const app = state.apps.find(function (item) { return item.id === id; });
        if (!app) {
            return;
        }
        api("GET", "/api/v1/admin/apps/" + id + "/channels").then(function (bound) {
            const boundSet = new Set(bound.map(Number));
            const rows = state.channels.map(function (channel) {
                return "<label class=\"check-row\"><input type=\"checkbox\" value=\"" + channel.id + "\""
                    + (boundSet.has(channel.id) ? " checked" : "") + ">"
                    + "<span>" + esc(channel.name) + " · " + esc(channel.code) + "</span></label>";
            }).join("");
            openModal("绑定渠道 · " + app.appId,
                    "<div class=\"field full\"><div class=\"check-list\">" + rows + "</div></div>",
                    function () {
                        const checks = Array.prototype.slice.call(document.querySelectorAll(".check-list input:checked"))
                                .map(function (input) { return Number(input.value); });
                        const operations = [];
                        state.channels.forEach(function (channel) {
                            const current = boundSet.has(channel.id);
                            const target = checks.indexOf(channel.id) !== -1;
                            if (current !== target) {
                                operations.push(target
                                    ? api("POST", "/api/v1/admin/apps/" + id + "/channels/" + channel.id)
                                    : api("DELETE", "/api/v1/admin/apps/" + id + "/channels/" + channel.id));
                            }
                        });
                        Promise.all(operations).then(function () { closeModal(); showToast("绑定已更新"); });
                    });
        });
    }

    function publishApp(id) {
        const app = state.apps.find(function (item) { return item.id === id; });
        if (!app) {
            return;
        }
        api("POST", "/api/v1/apps/" + encodeURIComponent(app.appId) + "/publish?operator=admin")
            .then(function (result) {
                showToast("已发布，版本 v" + result.version);
                loadAll();
            })
            .catch(function (error) {
                showToast(error.message, "err");
            });
    }

    function deleteApp(id) {
        confirmDelete("/api/v1/admin/apps/" + id, loadAll);
    }

    function deleteProvider(id) {
        confirmDelete("/api/v1/admin/providers/" + id, loadAll);
    }

    function deleteChannel(id) {
        confirmDelete("/api/v1/admin/channels/" + id, loadAll);
    }

    function deleteEndpoint(id) {
        confirmDelete("/api/v1/admin/endpoints/" + id, loadEndpoints);
    }

    function confirmDelete(url, done) {
        if (!window.confirm("确认删除？")) {
            return;
        }
        api("DELETE", url).then(function () { showToast("已删除"); done(); })
            .catch(function (error) { showToast(error.message, "err"); });
    }

    function appForm(app) {
        return "<form id=\"appForm\" class=\"modal-body\">"
            + textField("appId", "App ID", app && app.appId, !app)
            + textField("appName", "应用名称", app && app.appName)
            + textField("appSecret", "应用密钥", app && app.appSecret)
            + textField("remark", "备注", app && app.remark, false, true)
            + toggleField("enabled", "启用", app ? app.enabled : true)
            + "</form>";
    }

    function providerForm(provider) {
        return "<form id=\"providerForm\" class=\"modal-body\">"
            + textField("code", "编码", provider && provider.code)
            + textField("name", "名称", provider && provider.name)
            + textField("category", "分类", provider && (provider.category || "OTHER"))
            + textField("remark", "备注", provider && provider.remark, false, true)
            + toggleField("enabled", "启用", provider ? provider.enabled : true)
            + "</form>";
    }

    function channelForm(channel) {
        const options = state.providers.map(function (provider) {
            return "<option value=\"" + provider.id + "\""
                + (channel && channel.providerId === provider.id ? " selected" : "") + ">"
                + esc(provider.name) + " · " + esc(provider.code) + "</option>";
        }).join("");
        return "<form id=\"channelForm\" class=\"modal-body\">"
            + "<div class=\"field\"><label>服务商</label><select name=\"providerId\">" + options + "</select></div>"
            + textField("code", "渠道编码", channel && channel.code)
            + textField("name", "渠道名称", channel && channel.name)
            + textField("baseUrl", "Base URL", channel && channel.baseUrl)
            + textField("environment", "环境", channel && (channel.environment || "prod"))
            + toggleField("enabled", "启用", channel ? channel.enabled : true)
            + "</form>";
    }

    function endpointForm(endpoint) {
        return "<form id=\"endpointForm\" class=\"modal-body\">"
            + textField("code", "接口编码", endpoint && endpoint.code)
            + textField("name", "接口名称", endpoint && endpoint.name)
            + textField("httpMethod", "HTTP 方法", endpoint && (endpoint.httpMethod || "POST"))
            + textField("path", "路径", endpoint && endpoint.path)
            + textField("timeoutMs", "超时 ms", endpoint && endpoint.timeoutMs)
            + textField("retryMax", "重试次数", endpoint && endpoint.retryMax)
            + textField("retryBackoffMs", "重试间隔 ms", endpoint && endpoint.retryBackoffMs)
            + textField("circuitBreakerRatio", "熔断阈值", endpoint && endpoint.circuitBreakerRatio)
            + toggleField("enabled", "启用", endpoint ? endpoint.enabled : true)
            + "</form>";
    }

    function textField(name, label, value, required, full) {
        return "<div class=\"field" + (full ? " full" : "") + "\"><label>" + label + "</label>"
            + "<input name=\"" + name + "\" value=\"" + esc(value == null ? "" : value) + "\""
            + (required ? " required" : "") + "></div>";
    }

    function toggleField(name, label, checked) {
        return "<div class=\"field\"><label>" + label + "</label>"
            + "<select name=\"" + name + "\"><option value=\"true\"" + (checked ? " selected" : "") + ">启用</option>"
            + "<option value=\"false\"" + (!checked ? " selected" : "") + ">停用</option></select></div>";
    }

    function readForm(formId) {
        const form = document.getElementById(formId);
        const data = {};
        Array.prototype.slice.call(form.querySelectorAll("[name]")).forEach(function (input) {
            let value = input.value;
            if (input.type === "checkbox") {
                value = input.checked;
            } else if (input.tagName === "SELECT") {
                value = input.value === "true" ? true : (input.value === "false" ? false : input.value);
            }
            data[input.name] = value;
        });
        return data;
    }

    function openModal(title, bodyHtml, onSave) {
        state.modal = { onSave: onSave };
        document.getElementById("modalTitle").textContent = title;
        document.getElementById("modalBody").innerHTML = bodyHtml;
        document.getElementById("modal").hidden = false;
        refreshIcons();
    }

    function closeModal() {
        state.modal = null;
        document.getElementById("modal").hidden = true;
    }

    function showToast(message, type) {
        const toast = document.getElementById("toast");
        toast.textContent = message;
        toast.className = "toast " + (type || "ok");
        toast.hidden = false;
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(function () {
            toast.hidden = true;
        }, 2600);
    }

    function attachActions(bodyId, handlers) {
        const body = document.getElementById(bodyId);
        body.querySelectorAll("button[data-action]").forEach(function (button) {
            button.addEventListener("click", function () {
                const action = button.dataset.action;
                const id = Number(button.dataset.id);
                if (handlers[action]) {
                    handlers[action](id);
                }
            });
        });
    }

    function actionButton(action, label, id) {
        const className = action === "delete" ? "btn small danger" : "btn small";
        const icons = {
            edit: "pencil",
            bind: "link",
            publish: "send",
            delete: "trash-2",
            endpoints: "file-code"
        };
        return "<button class=\"" + className + "\" data-action=\"" + action + "\" data-id=\"" + id + "\">"
            + "<i data-lucide=\"" + icons[action] + "\"></i>" + label + "</button>";
    }

    function maskSecret(value) {
        if (!value) {
            return "—";
        }
        return "••••••";
    }

    function refreshIcons() {
        if (window.lucide) {
            window.lucide.createIcons();
        }
    }

    function esc(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    async function api(method, url, body) {
        const options = { method: method, headers: { "Content-Type": "application/json" } };
        if (body !== undefined) {
            options.body = JSON.stringify(body);
        }
        const response = await fetch(url, options);
        if (response.status === 304) {
            return null;
        }
        if (!response.ok) {
            let message = response.statusText;
            try {
                const payload = await response.json();
                message = payload.message || message;
            } catch (e) {
                // keep status text
            }
            throw new Error(message);
        }
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    }
})();
