(function () {
    const monthSelect = document.querySelector("#history-month");
    const yearSelect = document.querySelector("#history-year");
    const list = document.querySelector("#history-list");
    const heading = document.querySelector("#history-heading");
    const total = document.querySelector("#history-total");

    function escapeHtml(value) {
        return String(value).replace(/[&<>"']/g, (character) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[character]);
    }

    function transactionTypeLabel(type) {
        if (type === "fixed") return "Despesa fixa";
        if (type === "variable") return "Despesa avulsa";
        return "Renda";
    }

    async function setYears() {
        const currentYear = Number(FinanceStore.currentMonthKey().slice(0, 4));
        const years = new Set([currentYear]);

        (await FinanceStore.getTransactions({})).forEach((transaction) => {
            years.add(Number(transaction.date.slice(0, 4)));
        });

        yearSelect.innerHTML = [...years]
            .sort((first, second) => second - first)
            .map((year) => `<option value="${year}">${year}</option>`)
            .join("");
        yearSelect.value = String(currentYear);
    }

    async function render() {
        const month = Number(monthSelect.value);
        const year = Number(yearSelect.value);
        let transactions;
        try {
            transactions = await FinanceStore.getTransactions({ month, year });
        } catch (error) {
            list.innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
            return;
        }
        const monthKey = `${year}-${String(month).padStart(2, "0")}`;
        heading.textContent = `Movimentações de ${FinanceStore.formatMonth(monthKey)}`;

        const net = transactions.reduce((sum, transaction) => {
            const amount = transaction.type === "income"
                ? transaction.amountCents
                : -transaction.amountCents;
            return sum + amount;
        }, 0);
        total.textContent = `Saldo do período: ${FinanceStore.formatCurrency(net)}`;

        if (!transactions.length) {
            list.innerHTML = '<p class="empty-state">Nenhuma movimentação encontrada neste período.</p>';
            return;
        }

        list.innerHTML = transactions.map((transaction) => {
            const isIncome = transaction.type === "income";
            const category = transaction.category
                ? ` · ${escapeHtml(transaction.category)}`
                : "";
            const sign = isIncome ? "+" : "−";
            const amountClass = isIncome ? "positive" : "negative";

            return `
                <article class="record-card">
                    <div class="record-main">
                        <strong class="record-title">${escapeHtml(transaction.description)}</strong>
                        <small class="record-meta">${transactionTypeLabel(transaction.type)}${category} · ${FinanceStore.formatDate(transaction.date)}</small>
                    </div>
                    <strong class="record-amount ${amountClass}">${sign} ${FinanceStore.formatCurrency(transaction.amountCents)}</strong>
                </article>
            `;
        }).join("");
    }

    const currentMonth = FinanceStore.currentMonthKey().split("-")[1];
    monthSelect.value = String(Number(currentMonth));
    document.querySelector("#apply-filter").addEventListener("click", render);
    monthSelect.addEventListener("change", render);
    yearSelect.addEventListener("change", render);
    setYears().then(render, render);
})();
