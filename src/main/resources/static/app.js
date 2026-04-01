(function () {
  const TOKEN_KEY = "bacnaoday_token";
  const USERNAME_KEY = "bacnaoday_username";

  const $ = (id) => document.getElementById(id);

  const views = {
    login: $("view-login"),
    dashboard: $("view-dashboard"),
    page: $("view-page"),
  };

  function getToken() {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  function setSession(username, token) {
    sessionStorage.setItem(USERNAME_KEY, username);
    sessionStorage.setItem(TOKEN_KEY, token);
  }

  function clearSession() {
    sessionStorage.removeItem(USERNAME_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
  }

  function authHeaders() {
    const token = getToken();
    const h = { "Content-Type": "application/json" };
    if (token) h.Authorization = "Bearer " + token;
    return h;
  }

  async function parseError(res) {
    try {
      const body = await res.json();
      if (body && typeof body.message === "string") return body.message;
      if (body && typeof body.error === "string") return body.error;
    } catch {
      /* ignore */
    }
    return res.statusText || "Request failed";
  }

  async function api(path, options = {}) {
    const res = await fetch(path, {
      ...options,
      headers: { ...authHeaders(), ...options.headers },
    });
    if (res.status === 401) {
      clearSession();
      showLogin();
      throw new Error("Session expired. Sign in again.");
    }
    return res;
  }

  function showView(name) {
    Object.values(views).forEach((el) => el.classList.add("hidden"));
    views[name].classList.remove("hidden");
    const loggedIn = name !== "login";
    $("header-user").classList.toggle("hidden", !loggedIn);
    $("btn-logout").classList.toggle("hidden", !loggedIn);
    if (loggedIn) {
      $("header-user").textContent = sessionStorage.getItem(USERNAME_KEY) || "";
    }
  }

  function showLogin() {
    showView("login");
    $("login-error").classList.add("hidden");
  }

  function parseHash() {
    const h = (location.hash || "#/").replace(/^#/, "") || "/";
    const parts = h.split("/").filter(Boolean);
    if (parts[0] === "page" && parts[1] && /^\d+$/.test(parts[1])) {
      return { route: "page", pageId: parts[1] };
    }
    return { route: "dashboard" };
  }

  async function route() {
    if (!getToken()) {
      showLogin();
      return;
    }
    const { route, pageId } = parseHash();
    if (route === "page") {
      await openPage(pageId);
    } else {
      await loadDashboard();
    }
  }

  $("form-login").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const username = String(fd.get("username") || "").trim();
    const password = String(fd.get("password") || "");
    const errEl = $("login-error");
    errEl.classList.add("hidden");
    try {
      const res = await fetch("/api/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      if (!res.ok) {
        errEl.textContent = await parseError(res);
        errEl.classList.remove("hidden");
        return;
      }
      const data = await res.json();
      setSession(data.username, data.accessToken);
      if (!location.hash || location.hash === "#") {
        location.hash = "#/";
      }
      await route();
    } catch (ex) {
      errEl.textContent = ex.message || "Sign in failed";
      errEl.classList.remove("hidden");
    }
  });

  $("btn-logout").addEventListener("click", async () => {
    try {
      await api("/api/logout", { method: "POST", body: "{}" });
    } catch {
      /* still clear client */
    }
    clearSession();
    location.hash = "";
    showLogin();
  });

  async function loadDashboard() {
    showView("dashboard");
    $("dashboard-error").classList.add("hidden");
    $("create-page-error").classList.add("hidden");
    const listEl = $("page-list");
    const emptyEl = $("page-list-empty");
    listEl.replaceChildren();

    try {
      const res = await api("/api/relation-pages");
      if (!res.ok) {
        $("dashboard-error").textContent = await parseError(res);
        $("dashboard-error").classList.remove("hidden");
        return;
      }
      const pages = await res.json();
      if (!pages.length) {
        emptyEl.classList.remove("hidden");
        return;
      }
      emptyEl.classList.add("hidden");
      for (const p of pages) {
        const li = document.createElement("li");
        const a = document.createElement("a");
        a.href = "#/page/" + p.id;
        a.appendChild(document.createTextNode(p.name));
        const created = p.createdAt ? new Date(p.createdAt).toLocaleString() : "";
        if (created) {
          a.appendChild(document.createElement("br"));
          const meta = document.createElement("span");
          meta.className = "meta";
          meta.textContent = "Created " + created;
          a.appendChild(meta);
        }
        li.appendChild(a);
        listEl.appendChild(li);
      }
    } catch (ex) {
      $("dashboard-error").textContent = ex.message;
      $("dashboard-error").classList.remove("hidden");
    }
  }

  $("form-create-page").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const name = String(fd.get("name") || "").trim();
    const errEl = $("create-page-error");
    errEl.classList.add("hidden");
    if (!name) return;
    try {
      const res = await api("/api/relation-pages", {
        method: "POST",
        body: JSON.stringify({ name }),
      });
      if (!res.ok) {
        errEl.textContent = await parseError(res);
        errEl.classList.remove("hidden");
        return;
      }
      e.target.reset();
      await loadDashboard();
    } catch (ex) {
      errEl.textContent = ex.message;
      errEl.classList.remove("hidden");
    }
  });

  async function openPage(pageId) {
    showView("page");
    $("page-error").classList.add("hidden");
    $("page-title").textContent = "";
    $("page-meta").textContent = "";
    $("person-list").replaceChildren();
    $("person-list-empty").classList.add("hidden");

    try {
      const res = await api("/api/relation-pages/" + pageId);
      if (!res.ok) {
        $("page-error").textContent = await parseError(res);
        $("page-error").classList.remove("hidden");
        return;
      }
      const page = await res.json();
      $("page-title").textContent = page.name;
      const created = page.createdAt ? new Date(page.createdAt).toLocaleString() : "";
      $("page-meta").textContent = created ? "Created " + created : "";

      const pres = await api("/api/relation-pages/" + pageId + "/persons");
      if (!pres.ok) {
        $("page-error").textContent = await parseError(pres);
        $("page-error").classList.remove("hidden");
        return;
      }
      const persons = await pres.json();
      const ul = $("person-list");
      if (!persons.length) {
        $("person-list-empty").classList.remove("hidden");
      } else {
        for (const person of persons) {
          const li = document.createElement("li");
          li.textContent = person.displayName;
          ul.appendChild(li);
        }
      }
    } catch (ex) {
      $("page-error").textContent = ex.message;
      $("page-error").classList.remove("hidden");
    }
  }

  window.addEventListener("hashchange", () => {
    if (getToken()) route();
  });

  if (getToken()) {
    route();
  } else {
    showLogin();
  }
})();
