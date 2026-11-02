(function () {
    const sidebarSlot = document.querySelector("#sidebar-slot");
    const backdrop = document.querySelector("#sidebar-backdrop");
    const menuButton = document.querySelector("#menu-toggle");
    const page = document.body.dataset.page;

    const navigation = [
        {
            key: "dashboard",
            label: "Dashboard",
            href: "dashboard.html",
            icon: "dashboard.svg",
        },
        {
            key: "fixed",
            label: "Despesas Fixas",
            href: "fixed-expenses.html",
            icon: "despesas_fixas.svg",
        },
        {
            key: "variable",
            label: "Despesas Avulsas",
            href: "variable-expenses.html",
            icon: "despesas_avulsas.svg",
        },
        {
            key: "incomes",
            label: "Rendas",
            href: "incomes.html",
            icon: "rendas_navbar.svg",
        },
        {
            key: "history",
            label: "Histórico",
            href: "history.html",
            history: true,
        },
        {
            key: "notes",
            label: "Anotações",
            href: "notes.html",
            icon: "anotacoes.svg",
        },
    ];

    function navIcon(item) {
        if (item.history) {
            return '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 12a9 9 0 1 0 2.64-6.36L3 8.3"></path><path d="M3 3v5.3h5.3M12 7v5l3.2 2"></path></svg>';
        }
        return `<img src="../assets/icons/${item.icon}" alt="" />`;
    }

    if (sidebarSlot) {
        sidebarSlot.innerHTML = `
            <aside class="app-sidebar" id="app-sidebar" aria-label="Menu principal">
                <a class="sidebar-brand" href="dashboard.html">
                    <img src="../assets/logo/logo-icon.svg" alt="" />
                    <span><strong>Meu Financeiro</strong><small>Controle pessoal</small></span>
                </a>
                <nav class="sidebar-navigation" aria-label="Navegação principal">
                    <ul>${navigation
                        .map(
                            (item) => `
                        <li><a class="navigation-item ${item.key === page ? "is-active" : ""}" href="${item.href}" ${item.key === page ? 'aria-current="page"' : ""}>
                            ${navIcon(item)}<span>${item.label}</span>${item.key === page ? '<i class="active-indicator" aria-hidden="true"></i>' : ""}
                        </a></li>`,
                        )
                        .join("")}
                    </ul>
                </nav>
                <a class="sidebar-profile" href="profile.html">
                    <span class="profile-avatar" id="profile-avatar">LM</span>
                    <span class="profile-copy"><strong id="profile-name">Lucas Martins</strong><small id="profile-month">Outubro de 2026</small></span>
                </a>
            </aside>`;
    }

    function setMenuOpen(isOpen) {
        document.body.classList.toggle("sidebar-open", isOpen);
        if (menuButton)
            menuButton.setAttribute("aria-expanded", String(isOpen));
        if (backdrop) backdrop.hidden = !isOpen;
    }

    menuButton?.addEventListener("click", () => {
        setMenuOpen(menuButton.getAttribute("aria-expanded") !== "true");
    });
    backdrop?.addEventListener("click", () => setMenuOpen(false));
    document
        .querySelector("#app-sidebar")
        ?.addEventListener("click", (event) => {
            if (event.target.closest("a")) setMenuOpen(false);
        });
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") setMenuOpen(false);
    });

    function showProfile(profile) {
        const name = document.querySelector("#profile-name");
        const avatar = document.querySelector("#profile-avatar");
        const month = document.querySelector("#profile-month");
        if (name) name.textContent = profile.name;
        if (avatar)
            avatar.textContent = profile.name
                .split(/\s+/)
                .slice(0, 2)
                .map((part) => part[0])
                .join("")
                .toUpperCase();
        if (month)
            month.textContent = FinanceStore.formatMonth(
                FinanceStore.currentMonthKey(),
            );
    }

    // Mostra o nome guardado localmente (rápido) e depois confirma com o servidor.
    async function updateProfileSummary() {
        if (!window.FinanceStore) return;
        const cached = FinanceStore.getCachedProfile();
        if (cached) showProfile(cached);
        try {
            showProfile(await FinanceStore.getProfile());
        } catch {
            // sem conexão: mantém o que já está na tela
        }
    }

    updateProfileSummary();
    window.addEventListener("fincontrol:profilechange", updateProfileSummary);
})();
