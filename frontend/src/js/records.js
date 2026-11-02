(function () {
    const page = document.body.dataset.page;
    const configs = {
        fixed: "Cadastrar despesa fixa",
        variable: "Cadastrar gasto avulso",
        incomes: "Cadastrar renda"
    };
    const form = document.querySelector("#record-form");
    const panel = document.querySelector("#form-panel");
    const list = document.querySelector("#record-list");
    const idInput = document.querySelector("#record-id");
    const descriptionInput = document.querySelector("#record-description");
    const amountInput = document.querySelector("#record-amount");
    const categoryInput = document.querySelector("#record-category");
    const dateInput = document.querySelector("#record-date");
    const message = document.querySelector("#form-message");

    function escapeHtml(value) {
        return String(value).replace(/[&<>"']/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[character]);
    }

    async function getRecords() {
        if (page === "fixed") return FinanceStore.getFixedExpenses();
        const type = page === "variable" ? "variable" : "income";
        return FinanceStore.getTransactionsByType(type);
    }

    async function render() {
        let records;
        try {
            records = await getRecords();
        } catch (error) {
            list.innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
            return;
        }
        if (!records.length) {
            list.innerHTML = '<p class="empty-state">Nenhum registro cadastrado.</p>';
            return;
        }
        list.innerHTML = records.map((record) => {
            const date = page === "fixed" ? record.dueDate : record.date;
            const category = record.category ? ` · ${escapeHtml(record.category)}` : "";
            const suffix = page === "fixed" ? `Mensal · vence ${FinanceStore.formatDate(date)}` : FinanceStore.formatDate(date);
            return `<article class="record-card">
                <div class="record-main"><strong class="record-title">${escapeHtml(record.description)}</strong><small class="record-meta">${escapeHtml(suffix)}${category}</small></div>
                <strong class="record-amount">${FinanceStore.formatCurrency(record.amountCents)}</strong>
                <div class="record-actions"><button class="text-button" type="button" data-edit-id="${escapeHtml(record.id)}">Editar</button><button class="danger-button" type="button" data-delete-id="${escapeHtml(record.id)}">Excluir</button></div>
            </article>`;
        }).join("");
    }

    function openForm(record) {
        form.reset();
        message.textContent = "";
        idInput.value = record?.id || "";
        descriptionInput.value = record?.description || "";
        amountInput.value = record ? (record.amountCents / 100).toFixed(2) : "";
        if (categoryInput) categoryInput.value = record?.category || "";
        const date = record
            ? (page === "fixed" ? record.dueDate : record.date)
            : FinanceStore.localDateString();
        dateInput.value = FinanceStore.formatDate(date);
        document.querySelector("#form-title").textContent = record ? "Editar registro" : configs[page];
        panel.hidden = false;
        descriptionInput.focus();
        panel.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function closeForm() {
        panel.hidden = true;
        form.reset();
        idInput.value = "";
        message.textContent = "";
    }

    document.querySelector("#toggle-form").addEventListener("click", () => openForm());
    document.querySelector("#cancel-form").addEventListener("click", closeForm);
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        message.textContent = "";
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        try {
            const date = FinanceStore.parseDateInput(dateInput.value);
            const values = {
                id: idInput.value || undefined,
                description: descriptionInput.value,
                amount: amountInput.value
            };
            if (page === "fixed") {
                await FinanceStore.saveFixedExpense({
                    ...values,
                    category: categoryInput.value,
                    dueDate: date
                });
            } else if (page === "variable") {
                await FinanceStore.saveExpense({ ...values, category: categoryInput.value, date });
            } else {
                await FinanceStore.saveIncome({ ...values, date });
            }
            closeForm();
            await render();
        } catch (error) {
            message.textContent = error.message;
        }
    });

    list.addEventListener("click", async (event) => {
        const editButton = event.target.closest("[data-edit-id]");
        const deleteButton = event.target.closest("[data-delete-id]");
        if (editButton) {
            const id = editButton.dataset.editId;
            const record = (await getRecords()).find((item) => item.id === id);
            if (record) openForm(record);
        }
        const confirmationMessage = page === "fixed"
            ? "Excluir esta despesa fixa e todas as ocorrências do histórico?"
            : "Excluir este registro?";

        if (deleteButton && window.confirm(confirmationMessage)) {
            try {
                if (page === "fixed") await FinanceStore.deleteFixedExpense(deleteButton.dataset.deleteId);
                else await FinanceStore.deleteTransaction(deleteButton.dataset.deleteId);
                await render();
            } catch (error) {
                window.alert(error.message);
            }
        }
    });

    render();
})();
