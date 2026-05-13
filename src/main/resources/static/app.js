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
    if (name !== "page") {
      closeCreateRelationDialog();
      closeDeleteRelationDialog();
    }
    const mainEl = document.querySelector("main.main");
    if (mainEl) mainEl.classList.toggle("main--wide", name === "page");
    const loggedIn = name !== "login";
    $("header-user").classList.toggle("hidden", !loggedIn);
    $("btn-logout").classList.toggle("hidden", !loggedIn);
    if (loggedIn) {
      $("header-user").textContent = sessionStorage.getItem(USERNAME_KEY) || "";
    }
  }

  function formatRelationType(t) {
    if (!t) return "";
    return t
      .replace(/_/g, " ")
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  /** At most one edge per unordered pair of people (hides inverse / second directed row). */
  function dedupeOneEdgePerNodePair(edges) {
    const seen = new Set();
    const out = [];
    for (const e of edges) {
      const a = Math.min(e.fromPersonId, e.toPersonId);
      const b = Math.max(e.fromPersonId, e.toPersonId);
      const key = a + "|" + b;
      if (seen.has(key)) continue;
      seen.add(key);
      out.push(e);
    }
    return out;
  }

  /**
   * Layered tree-style layout: ancestors above descendants; spouses/siblings share a row;
   * multiple people on the same generation are spread horizontally.
   */
  function layoutRelationGraphLayers(nodes, allEdges, w, h, pad) {
    const ids = new Set(nodes.map((n) => n.id));
    const rank = new Map();
    for (const n of nodes) {
      rank.set(n.id, 0);
    }

    const parentChild = [];
    const sameGeneration = [];
    for (const e of allEdges) {
      const a = e.fromPersonId;
      const b = e.toPersonId;
      if (!ids.has(a) || !ids.has(b)) continue;
      const t = e.relationType;
      if (t === "FATHER_OF" || t === "MOTHER_OF" || t === "PARENT_OF") {
        parentChild.push([a, b]);
      } else if (t === "SON_OF" || t === "DAUGHTER_OF" || t === "CHILD_OF") {
        parentChild.push([b, a]);
      } else if (t === "SPOUSE_OF" || t === "SIBLING_OF") {
        sameGeneration.push([a, b]);
      }
    }

    const relaxSteps = Math.max(nodes.length, 1);
    for (let s = 0; s < relaxSteps; s++) {
      for (const [p, c] of parentChild) {
        const next = rank.get(p) + 1;
        if (next > rank.get(c)) {
          rank.set(c, next);
        }
      }
    }

    let merged = true;
    let guard = 0;
    while (merged && guard < relaxSteps * 2) {
      merged = false;
      guard++;
      for (const [a, b] of sameGeneration) {
        const m = Math.max(rank.get(a), rank.get(b));
        if (rank.get(a) !== m || rank.get(b) !== m) {
          rank.set(a, m);
          rank.set(b, m);
          merged = true;
        }
      }
    }

    let maxR = 0;
    for (const n of nodes) {
      const r = rank.get(n.id);
      if (r > maxR) maxR = r;
    }

    const layers = new Map();
    for (const n of nodes) {
      const r = rank.get(n.id);
      if (!layers.has(r)) {
        layers.set(r, []);
      }
      layers.get(r).push(n);
    }
    for (const list of layers.values()) {
      list.sort((a, b) => a.id - b.id);
    }

    const innerW = w - 2 * pad;
    const innerH = h - 2 * pad;
    const rowCount = maxR + 1;
    const rowGap =
      rowCount <= 1 ? 0 : Math.min(96, innerH / Math.max(rowCount - 1, 1));
    const yBase = pad + (innerH - (rowCount - 1) * rowGap) / 2;

    const positions = new Map();
    for (let r = 0; r <= maxR; r++) {
      const row = layers.get(r) || [];
      const count = row.length;
      const colGap =
        count <= 1 ? 0 : Math.min(160, innerW / Math.max(count - 1, 1));
      const rowSpan = count <= 1 ? 0 : (count - 1) * colGap;
      const x0 = w / 2 - rowSpan / 2;
      const y = yBase + r * rowGap;
      row.forEach((node, i) => {
        positions.set(node.id, { x: x0 + i * colGap, y });
      });
    }
    return positions;
  }

  let personGraphMarkInFlight = false;
  /** Cleans document listeners used to dismiss the per-node hover menu across re-renders. */
  let relationGraphMenuDismiss = null;

  const RELATION_NODE_MENU_HIDE_MS = 260;
  const TOAST_HIDE_MS = 2600;
  let toastHideTimer = null;

  function showToast(message) {
    const toastEl = $("app-toast");
    if (!toastEl) return;
    toastEl.textContent = message;
    toastEl.classList.remove("hidden");
    if (toastHideTimer !== null) {
      clearTimeout(toastHideTimer);
    }
    toastHideTimer = setTimeout(() => {
      toastEl.classList.add("hidden");
      toastHideTimer = null;
    }, TOAST_HIDE_MS);
  }

  function closeCreateRelationDialog() {
    const modalEl = $("create-relation-modal");
    if (!modalEl || modalEl.classList.contains("hidden")) return;
    modalEl.classList.add("hidden");
    $("form-create-relation").reset();
    $("create-relation-error").classList.add("hidden");
    $("create-relation-context").textContent = "Create a related person from the selected node.";
    delete modalEl.dataset.pageId;
    delete modalEl.dataset.toPersonId;
  }

  function openCreateRelationDialog(pageId, toPersonId, personLabel) {
    const modalEl = $("create-relation-modal");
    if (!modalEl) return;
    const pageIdNum = Number(pageId);
    const toPersonIdNum = Number(toPersonId);
    if (!Number.isFinite(pageIdNum) || !Number.isFinite(toPersonIdNum)) return;
    const trimmedLabel = typeof personLabel === "string" ? personLabel.trim() : "";
    $("form-create-relation").reset();
    $("create-relation-error").classList.add("hidden");
    modalEl.dataset.pageId = String(pageIdNum);
    modalEl.dataset.toPersonId = String(toPersonIdNum);
    $("create-relation-context").textContent = trimmedLabel
      ? 'Create a related person from "' + trimmedLabel + '".'
      : "Create a related person from the selected node.";
    modalEl.classList.remove("hidden");
    requestAnimationFrame(() => {
      $("input-create-relation-display-name").focus();
    });
  }

  function closeDeleteRelationDialog() {
    const modalEl = $("delete-relation-modal");
    if (!modalEl || modalEl.classList.contains("hidden")) return;
    modalEl.classList.add("hidden");
    $("delete-relation-error").classList.add("hidden");
    $("delete-relation-context").textContent = "Delete the selected person from this relation page?";
    delete modalEl.dataset.pageId;
    delete modalEl.dataset.personId;
    delete modalEl.dataset.personLabel;
  }

  function openDeleteRelationDialog(pageId, personId, personLabel) {
    const modalEl = $("delete-relation-modal");
    if (!modalEl) return;
    const pageIdNum = Number(pageId);
    const personIdNum = Number(personId);
    if (!Number.isFinite(pageIdNum) || !Number.isFinite(personIdNum)) return;
    const trimmedLabel = typeof personLabel === "string" ? personLabel.trim() : "";
    $("delete-relation-error").classList.add("hidden");
    modalEl.dataset.pageId = String(pageIdNum);
    modalEl.dataset.personId = String(personIdNum);
    modalEl.dataset.personLabel = trimmedLabel;
    $("delete-relation-context").textContent = trimmedLabel
      ? 'Delete "' + trimmedLabel + '" from this relation page?'
      : "Delete the selected person from this relation page?";
    modalEl.classList.remove("hidden");
    requestAnimationFrame(() => {
      $("btn-delete-relation-cancel").focus();
    });
  }

  async function loadPersonGraphPage(pageId, container) {
    const gres = await api("/api/relation-pages/" + pageId + "/persons/graph");
    if (!gres.ok) {
      $("page-error").textContent = await parseError(gres);
      $("page-error").classList.remove("hidden");
      return false;
    }
    $("page-error").classList.add("hidden");
    const graph = await gres.json();
    renderPersonGraph(container, graph, pageId);
    return true;
  }

  async function putMarkedPerson(pageId, personId, container) {
    const res = await api("/api/relation-pages/" + pageId + "/marked-person", {
      method: "PUT",
      body: JSON.stringify({ personId }),
    });
    if (!res.ok) {
      $("page-error").textContent = await parseError(res);
      $("page-error").classList.remove("hidden");
      return;
    }
    $("page-error").classList.add("hidden");
    await loadPersonGraphPage(pageId, container);
  }

  function renderPersonGraph(container, graph, pageId) {
    if (relationGraphMenuDismiss) {
      relationGraphMenuDismiss.abort();
      relationGraphMenuDismiss = null;
    }

    container.replaceChildren();
    const nodes = graph.nodes || [];
    const edges = dedupeOneEdgePerNodePair(graph.edges || []);
    if (!nodes.length) {
      const empty = document.createElement("p");
      empty.className = "muted relation-graph-empty";
      empty.textContent = "No people yet.";
      container.appendChild(empty);
      return;
    }

    const interactive = pageId != null && pageId !== "";

    const svgNS = "http://www.w3.org/2000/svg";
    const wrap = document.createElement("div");
    wrap.className = "relation-graph";

    const svg = document.createElementNS(svgNS, "svg");
    const w = 640;
    const h = 420;
    const pad = 80;
    svg.setAttribute("viewBox", "0 0 " + w + " " + h);
    svg.setAttribute("class", "relation-graph-svg");
    if (interactive) {
      svg.setAttribute("role", "presentation");
    } else {
      svg.setAttribute("role", "img");
      svg.setAttribute("aria-label", "People and relations on this page");
    }

    const defs = document.createElementNS(svgNS, "defs");
    const marker = document.createElementNS(svgNS, "marker");
    marker.setAttribute("id", "relation-graph-arrow");
    marker.setAttribute("markerWidth", "10");
    marker.setAttribute("markerHeight", "7");
    marker.setAttribute("refX", "9");
    marker.setAttribute("refY", "3.5");
    marker.setAttribute("orient", "auto");
    const poly = document.createElementNS(svgNS, "polygon");
    poly.setAttribute("points", "0 0, 10 3.5, 0 7");
    poly.setAttribute("class", "relation-graph-arrowhead");
    marker.appendChild(poly);
    defs.appendChild(marker);
    svg.appendChild(defs);

    const positions = layoutRelationGraphLayers(nodes, graph.edges || [], w, h, pad);

    const edgeLabels = [];
    for (const e of edges) {
      const from = positions.get(e.fromPersonId);
      const to = positions.get(e.toPersonId);
      if (!from || !to) continue;
      const line = document.createElementNS(svgNS, "line");
      line.setAttribute("x1", from.x);
      line.setAttribute("y1", from.y);
      line.setAttribute("x2", to.x);
      line.setAttribute("y2", to.y);
      line.setAttribute("class", "relation-graph-edge");
      line.setAttribute("marker-end", "url(#relation-graph-arrow)");
      svg.appendChild(line);

      const mx = (from.x + to.x) / 2;
      const my = (from.y + to.y) / 2;
      const label = document.createElementNS(svgNS, "text");
      label.setAttribute("x", mx);
      label.setAttribute("y", my);
      label.setAttribute("class", "relation-graph-edge-label");
      label.setAttribute("text-anchor", "middle");
      label.textContent = formatRelationType(e.relationType);
      edgeLabels.push(label);
    }

    const markedId =
      graph.markedPersonId != null && graph.markedPersonId !== undefined
        ? Number(graph.markedPersonId)
        : null;

    let nodeHoverMenuUi = null;
    if (interactive) {
      let menuHideTimer = null;
      let openMenuAnchor = null;

      const lookupMsgEl = document.createElement("p");
      lookupMsgEl.className = "relation-graph-lookup-msg muted small hidden";
      lookupMsgEl.setAttribute("role", "status");

      const menuEl = document.createElement("div");
      menuEl.className = "relation-node-menu";
      menuEl.setAttribute("role", "menu");
      menuEl.setAttribute("aria-label", "Person actions");

      const btnMarkPerson = document.createElement("button");
      btnMarkPerson.type = "button";
      btnMarkPerson.className = "relation-node-menu__btn";
      btnMarkPerson.setAttribute("role", "menuitem");
      btnMarkPerson.textContent = "Mark person";

      const btnShowRelation = document.createElement("button");
      btnShowRelation.type = "button";
      btnShowRelation.className = "relation-node-menu__btn";
      btnShowRelation.setAttribute("role", "menuitem");
      btnShowRelation.textContent = "Show relation";

      const btnCreateRelation = document.createElement("button");
      btnCreateRelation.type = "button";
      btnCreateRelation.className = "relation-node-menu__btn";
      btnCreateRelation.setAttribute("role", "menuitem");
      btnCreateRelation.textContent = "Create relation";

      const btnDeleteRelation = document.createElement("button");
      btnDeleteRelation.type = "button";
      btnDeleteRelation.className = "relation-node-menu__btn relation-node-menu__btn--danger";
      btnDeleteRelation.setAttribute("role", "menuitem");
      btnDeleteRelation.textContent = "Delete relation";

      menuEl.append(btnMarkPerson, btnShowRelation, btnCreateRelation, btnDeleteRelation);

      function cancelScheduledMenuHide() {
        if (menuHideTimer !== null) {
          clearTimeout(menuHideTimer);
          menuHideTimer = null;
        }
      }

      function closePersonNodeMenu() {
        cancelScheduledMenuHide();
        menuEl.classList.remove("is-open");
        if (openMenuAnchor) {
          openMenuAnchor.setAttribute("aria-expanded", "false");
        }
        openMenuAnchor = null;
      }

      function schedulePersonNodeMenuHide() {
        cancelScheduledMenuHide();
        menuHideTimer = setTimeout(() => {
          menuHideTimer = null;
          closePersonNodeMenu();
        }, RELATION_NODE_MENU_HIDE_MS);
      }

      function positionHoverMenu(anchorEl) {
        const rect = anchorEl.getBoundingClientRect();
        menuEl.style.left = Math.round(rect.left + rect.width / 2) + "px";
        menuEl.style.top = Math.round(rect.bottom + 8) + "px";
        menuEl.style.transform = "translateX(-50%)";
      }

      function openPersonNodeMenu(anchorEl, nodeId, displayLabel) {
        cancelScheduledMenuHide();
        if (openMenuAnchor && openMenuAnchor !== anchorEl) {
          openMenuAnchor.setAttribute("aria-expanded", "false");
        }
        openMenuAnchor = anchorEl;
        openMenuAnchor.setAttribute("aria-expanded", "true");
        menuEl.dataset.personId = String(nodeId);
        menuEl.dataset.personLabel = displayLabel;
        lookupMsgEl.classList.add("hidden");
        positionHoverMenu(anchorEl);
        menuEl.classList.add("is-open");
      }

      menuEl.addEventListener("mouseenter", cancelScheduledMenuHide);
      menuEl.addEventListener("mouseleave", schedulePersonNodeMenuHide);

      btnMarkPerson.addEventListener("click", async () => {
        const pid = Number(menuEl.dataset.personId, 10);
        if (!Number.isFinite(pid)) return;
        closePersonNodeMenu();
        if (personGraphMarkInFlight) return;
        personGraphMarkInFlight = true;
        try {
          await putMarkedPerson(pageId, pid, container);
        } finally {
          personGraphMarkInFlight = false;
        }
      });

      btnShowRelation.addEventListener("click", async () => {
        const pid = Number(menuEl.dataset.personId, 10);
        if (!Number.isFinite(pid)) return;
        const personLabel =
          typeof menuEl.dataset.personLabel === "string" ? menuEl.dataset.personLabel.trim() || "Person" : "Person";

        closePersonNodeMenu();

        const res = await api("/api/persons/" + pid + "/relative-level");
        if (!res.ok) {
          $("page-error").textContent = await parseError(res);
          $("page-error").classList.remove("hidden");
          return;
        }
        $("page-error").classList.add("hidden");
        const levelData = await res.json();

        lookupMsgEl.textContent =
          personLabel +
          ": relative level " +
          levelData.level +
          " from your marked person on this page.";
        lookupMsgEl.classList.remove("hidden");
      });

      btnCreateRelation.addEventListener("click", () => {
        const personLabel =
          typeof menuEl.dataset.personLabel === "string" ? menuEl.dataset.personLabel : "";
        const toPersonId = Number(menuEl.dataset.personId, 10);
        closePersonNodeMenu();
        openCreateRelationDialog(pageId, toPersonId, personLabel);
      });

      btnDeleteRelation.addEventListener("click", () => {
        const personLabel =
          typeof menuEl.dataset.personLabel === "string" ? menuEl.dataset.personLabel : "";
        const personId = Number(menuEl.dataset.personId, 10);
        closePersonNodeMenu();
        openDeleteRelationDialog(pageId, personId, personLabel);
      });

      relationGraphMenuDismiss = new AbortController();
      const dismissSignal = relationGraphMenuDismiss.signal;

      document.addEventListener(
        "pointerdown",
        (ev) => {
          if (!menuEl.classList.contains("is-open")) return;
          if (menuEl.contains(ev.target)) return;
          if (ev.target.closest && ev.target.closest("g.relation-graph-node") === openMenuAnchor) return;
          closePersonNodeMenu();
        },
        { capture: true, signal: dismissSignal }
      );

      document.addEventListener(
        "keydown",
        (ev) => {
          if (ev.key !== "Escape") return;
          if (!menuEl.classList.contains("is-open")) return;
          ev.preventDefault();
          const anchor = openMenuAnchor;
          closePersonNodeMenu();
          if (anchor && anchor.focus) anchor.focus();
        },
        { signal: dismissSignal }
      );

      nodeHoverMenuUi = {
        lookupMsgEl,
        menuEl,
        openPersonNodeMenu,
        closePersonNodeMenu,
        schedulePersonNodeMenuHide,
        cancelScheduledMenuHide,
      };
    }

    for (const node of nodes) {
      const pos = positions.get(node.id);
      if (!pos) continue;
      const g = document.createElementNS(svgNS, "g");
      const isMarked = markedId != null && Number(node.id) === markedId;
      let nodeClass =
        isMarked
          ? "relation-graph-node relation-graph-node--marked"
          : "relation-graph-node";
      if (interactive) {
        nodeClass += " relation-graph-node--interactive";
      }
      g.setAttribute("class", nodeClass);
      const display = node.displayName || "";
      const nodeIdNum = Number(node.id);
      if (interactive && nodeHoverMenuUi) {
        g.setAttribute("role", "group");
        g.setAttribute("tabindex", "0");
        g.setAttribute("aria-expanded", "false");
        g.setAttribute("aria-haspopup", "true");
        g.dataset.personId = String(nodeIdNum);
        g.setAttribute(
          "aria-label",
          (isMarked ? "Marked as your person: " : "Person: ") + display + "; open menu with hover or keyboard"
        );
        g.addEventListener("keydown", (ev) => {
          if ((ev.key === "Enter" || ev.key === " ") && document.activeElement === g) {
            ev.preventDefault();
            nodeHoverMenuUi.openPersonNodeMenu(g, nodeIdNum, display);
            nodeHoverMenuUi.menuEl.querySelector("button")?.focus();
          }
        });
        g.addEventListener("mouseenter", () => {
          nodeHoverMenuUi.openPersonNodeMenu(g, nodeIdNum, display);
        });
        g.addEventListener("mouseleave", () => {
          nodeHoverMenuUi.schedulePersonNodeMenuHide();
        });
        g.addEventListener("focusin", () => {
          nodeHoverMenuUi.openPersonNodeMenu(g, nodeIdNum, display);
        });
        g.addEventListener("focusout", () => {
          setTimeout(() => {
            if (nodeHoverMenuUi.menuEl.dataset.personId !== String(nodeIdNum)) return;
            const ae = document.activeElement;
            if (ae === g || nodeHoverMenuUi.menuEl.contains(ae)) return;
            nodeHoverMenuUi.closePersonNodeMenu();
          }, 0);
        });
      }
      const circle = document.createElementNS(svgNS, "circle");
      circle.setAttribute("cx", pos.x);
      circle.setAttribute("cy", pos.y);
      circle.setAttribute("r", "30");
      circle.setAttribute("class", "relation-graph-node-circle");
      g.appendChild(circle);
      const text = document.createElementNS(svgNS, "text");
      text.setAttribute("x", pos.x);
      text.setAttribute("y", pos.y + 5);
      text.setAttribute("class", "relation-graph-node-label");
      text.setAttribute("text-anchor", "middle");
      const raw = display;
      text.textContent = raw.length > 16 ? raw.slice(0, 14) + "…" : raw;
      g.appendChild(text);
      svg.appendChild(g);
    }

    for (const label of edgeLabels) {
      svg.appendChild(label);
    }

    wrap.appendChild(svg);
    if (nodeHoverMenuUi) {
      wrap.appendChild(nodeHoverMenuUi.lookupMsgEl);
      wrap.appendChild(nodeHoverMenuUi.menuEl);
    }
    container.appendChild(wrap);
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

  $("btn-create-relation-close").addEventListener("click", () => {
    closeCreateRelationDialog();
  });

  $("btn-create-relation-cancel").addEventListener("click", () => {
    closeCreateRelationDialog();
  });

  $("create-relation-modal").addEventListener("click", (ev) => {
    if (ev.target === ev.currentTarget) {
      closeCreateRelationDialog();
    }
  });

  $("btn-delete-relation-close").addEventListener("click", () => {
    closeDeleteRelationDialog();
  });

  $("btn-delete-relation-cancel").addEventListener("click", () => {
    closeDeleteRelationDialog();
  });

  $("delete-relation-modal").addEventListener("click", (ev) => {
    if (ev.target === ev.currentTarget) {
      closeDeleteRelationDialog();
    }
  });

  document.addEventListener("keydown", (ev) => {
    if (ev.key !== "Escape") return;
    if (!$("delete-relation-modal").classList.contains("hidden")) {
      ev.preventDefault();
      closeDeleteRelationDialog();
      return;
    }
    if (!$("create-relation-modal").classList.contains("hidden")) {
      ev.preventDefault();
      closeCreateRelationDialog();
    }
  });

  $("form-create-relation").addEventListener("submit", async (e) => {
    e.preventDefault();
    const modalEl = $("create-relation-modal");
    const fd = new FormData(e.target);
    const displayName = String(fd.get("displayName") || "").trim();
    const relationType = String(fd.get("relationType") || "").trim();
    const pageId = Number(modalEl.dataset.pageId);
    const toPersonId = Number(modalEl.dataset.toPersonId);
    const errEl = $("create-relation-error");
    const submitBtn = e.target.querySelector('button[type="submit"]');
    errEl.classList.add("hidden");
    if (!displayName || !relationType) return;
    if (!Number.isFinite(pageId) || !Number.isFinite(toPersonId)) {
      errEl.textContent = "Missing selected person for this relation.";
      errEl.classList.remove("hidden");
      return;
    }
    submitBtn.disabled = true;
    try {
      const res = await api("/api/relation-pages/" + pageId + "/persons", {
        method: "POST",
        body: JSON.stringify({ displayName, relationType, toPersonId }),
      });
      if (!res.ok) {
        errEl.textContent = await parseError(res);
        errEl.classList.remove("hidden");
        return;
      }
      const createdPerson = await res.json();
      closeCreateRelationDialog();
      await loadPersonGraphPage(pageId, $("person-graph"));
      showToast('Created relation for "' + (createdPerson.displayName || displayName) + '".');
    } catch (ex) {
      errEl.textContent = ex.message;
      errEl.classList.remove("hidden");
    } finally {
      submitBtn.disabled = false;
    }
  });

  $("form-delete-relation").addEventListener("submit", async (e) => {
    e.preventDefault();
    const modalEl = $("delete-relation-modal");
    const pageId = Number(modalEl.dataset.pageId);
    const personId = Number(modalEl.dataset.personId);
    const personLabel =
      typeof modalEl.dataset.personLabel === "string" ? modalEl.dataset.personLabel.trim() : "";
    const errEl = $("delete-relation-error");
    const submitBtn = e.target.querySelector('button[type="submit"]');
    errEl.classList.add("hidden");
    if (!Number.isFinite(pageId) || !Number.isFinite(personId)) {
      errEl.textContent = "Missing selected person for this deletion.";
      errEl.classList.remove("hidden");
      return;
    }
    submitBtn.disabled = true;
    try {
      const res = await api("/api/relation-page/" + pageId + "/" + personId, {
        method: "DELETE",
      });
      if (!res.ok) {
        errEl.textContent = await parseError(res);
        errEl.classList.remove("hidden");
        return;
      }
      closeDeleteRelationDialog();
      await loadPersonGraphPage(pageId, $("person-graph"));
      showToast(
        personLabel ? 'Deleted relation for "' + personLabel + '".' : "Deleted relation."
      );
    } catch (ex) {
      errEl.textContent = ex.message;
      errEl.classList.remove("hidden");
    } finally {
      submitBtn.disabled = false;
    }
  });

  async function openPage(pageId) {
    showView("page");
    closeCreateRelationDialog();
    closeDeleteRelationDialog();
    $("page-error").classList.add("hidden");
    $("page-title").textContent = "";
    $("page-meta").textContent = "";
    $("person-graph").replaceChildren();

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

      await loadPersonGraphPage(pageId, $("person-graph"));
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
