(function () {
    const balance = document.querySelector("#summary-balance");
    const fixedTotal = document.querySelector("#summary-fixed");
    const variableTotal = document.querySelector("#summary-variable");
    const incomeTotal = document.querySelector("#summary-income");
    const recentList = document.querySelector("#recent-list");

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

    async function render() {
        const summary = await FinanceStore.getDashboard();
        balance.textContent = FinanceStore.formatCurrency(summary.balance);
        fixedTotal.textContent = FinanceStore.formatCurrency(summary.fixed);
        variableTotal.textContent = FinanceStore.formatCurrency(summary.variable);
        incomeTotal.textContent = FinanceStore.formatCurrency(summary.income);

        const transactions = summary.recent;
        if (!transactions.length) {
            recentList.innerHTML = '<li class="empty-state">Nenhuma movimentação cadastrada.</li>';
            return;
        }

        recentList.innerHTML = transactions.map((transaction) => {
            const isIncome = transaction.type === "income";
            const sign = isIncome ? "+" : "−";
            const amountClass = isIncome ? "positive" : "negative";
            const category = transaction.category
                ? `${escapeHtml(transaction.category)} · `
                : "";

            return `
                <li class="transaction-row">
                    <span class="transaction-copy">
                        <strong>${escapeHtml(transaction.description)}</strong>
                        <small>${category}${transactionTypeLabel(transaction.type)} · ${FinanceStore.formatDate(transaction.date)}</small>
                    </span>
                    <span class="transaction-amount ${amountClass}">${sign} ${FinanceStore.formatCurrency(transaction.amountCents)}</span>
                </li>
            `;
        }).join("");
    }

    render().catch((error) => {
        recentList.innerHTML = `<li class="empty-state">${escapeHtml(error.message)}</li>`;
    });
})();
