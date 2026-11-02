(function () {
    const PROFILE_KEY = "fincontrol.profile"; // cópia local só para não piscar o nome no menu

    // Páginas internas exigem login (login.html e register.html não carregam este arquivo).
    Api.requireLogin();

    function localDateString(date = new Date()) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    function monthKey(dateString) {
        return dateString.slice(0, 7);
    }

    function currentMonthKey() {
        return monthKey(localDateString());
    }

    function parseDate(dateString) {
        const [year, month, day] = dateString.split("-").map(Number);
        return new Date(year, month - 1, day);
    }

    function parseDateInput(value) {
        const match = value.trim().match(/^(\d{2})\/(\d{2})\/(\d{4})$/);

        if (!match) {
            throw new Error("Use uma data no formato DD/MM/AAAA.");
        }

        const [, dayText, monthText, yearText] = match;
        const day = Number(dayText);
        const month = Number(monthText);
        const year = Number(yearText);
        const lastDay = new Date(year, month, 0).getDate();

        if (year < 1 || month < 1 || month > 12 || day < 1 || day > lastDay) {
            throw new Error("Informe uma data válida no formato DD/MM/AAAA.");
        }

        return `${yearText}-${monthText}-${dayText}`;
    }

    function amountToCents(value) {
        const amount = Number(value);
        if (!Number.isFinite(amount) || amount <= 0) {
            throw new Error("Informe um valor maior que zero.");
        }
        return Math.round(amount * 100);
    }

    function saveTransaction({ id, type, description, amount, category, date }) {
        const body = {
            type,
            description: description.trim(),
            amountCents: amountToCents(amount),
            category: category ? category.trim() : "",
            date
        };
        if (!body.description || !body.date) {
            throw new Error("Preencha a descrição e a data.");
        }
        return id
            ? Api.request("PUT", `/api/transactions/${id}`, body)
            : Api.request("POST", "/api/transactions", body);
    }

    function cacheProfile(profile) {
        localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
        return profile;
    }

    const FinanceStore = {
        currentMonthKey,
        localDateString,
        formatCurrency(cents) {
            return new Intl.NumberFormat("pt-BR", {
                style: "currency",
                currency: "BRL"
            }).format(cents / 100);
        },
        formatDate(dateString) {
            return parseDate(dateString).toLocaleDateString("pt-BR", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric"
            });
        },
        parseDateInput,
        formatMonth(month) {
            const [year, monthNumber] = month.split("-").map(Number);
            return new Intl.DateTimeFormat("pt-BR", {
                month: "long",
                year: "numeric"
            }).format(new Date(year, monthNumber - 1, 1));
        },

        // ---- Perfil ----
        getCachedProfile() {
            try {
                return JSON.parse(localStorage.getItem(PROFILE_KEY)) || null;
            } catch {
                return null;
            }
        },
        async getProfile() {
            return cacheProfile(await Api.request("GET", "/api/profile"));
        },
        async updateProfile(profile) {
            const name = profile.name.trim();
            const email = profile.email.trim().toLowerCase();
            if (!name || !email) throw new Error("Informe nome e e-mail.");

            cacheProfile(await Api.request("PUT", "/api/profile", { name, email }));
            window.dispatchEvent(new Event("fincontrol:profilechange"));
        },

        // ---- Dashboard: { balance, income, fixed, variable, recent: [...] } ----
        getDashboard() {
            return Api.request("GET", "/api/dashboard");
        },

        // ---- Transações (histórico, rendas e despesas avulsas) ----
        getTransactions({ month, year } = {}) {
            const params = new URLSearchParams();
            if (month) params.set("month", month);
            if (year) params.set("year", year);
            return Api.request("GET", `/api/transactions?${params}`);
        },
        getTransactionsByType(type) {
            return Api.request("GET", `/api/transactions?type=${type}`);
        },
        saveExpense({ id, description, amount, category, date }) {
            return saveTransaction({ id, type: "variable", description, amount, category, date });
        },
        saveIncome({ id, description, amount, date }) {
            return saveTransaction({ id, type: "income", description, amount, category: "", date });
        },
        deleteTransaction(id) {
            return Api.request("DELETE", `/api/transactions/${id}`);
        },

        // ---- Despesas fixas ----
        getFixedExpenses() {
            return Api.request("GET", "/api/fixed-expenses");
        },
        saveFixedExpense({ id, description, amount, category, dueDate }) {
            const body = {
                description: description.trim(),
                amountCents: amountToCents(amount),
                category: category.trim(),
                dueDate
            };
            if (!body.description || !body.category || !dueDate) {
                throw new Error("Preencha todos os campos.");
            }
            return id
                ? Api.request("PUT", `/api/fixed-expenses/${id}`, body)
                : Api.request("POST", "/api/fixed-expenses", body);
        },
        deleteFixedExpense(id) {
            return Api.request("DELETE", `/api/fixed-expenses/${id}`);
        },

        // ---- Anotações ----
        getNotes() {
            return Api.request("GET", "/api/notes");
        },
        saveNote({ id, title, content, date }) {
            const body = { title: title.trim(), content: content.trim(), date: date || "" };
            if (!body.title || !body.content) {
                throw new Error("Informe o título e o conteúdo da anotação.");
            }
            return id
                ? Api.request("PUT", `/api/notes/${id}`, body)
                : Api.request("POST", "/api/notes", body);
        },
        deleteNote(id) {
            return Api.request("DELETE", `/api/notes/${id}`);
        }
    };

    window.FinanceStore = FinanceStore;
})();
