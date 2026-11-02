(function () {
    const form = document.querySelector("#note-form");
    const panel = document.querySelector("#form-panel");
    const list = document.querySelector("#note-list");
    const idInput = document.querySelector("#note-id");
    const titleInput = document.querySelector("#note-title");
    const dateInput = document.querySelector("#note-date");
    const contentInput = document.querySelector("#note-content");
    const message = document.querySelector("#note-message");
    const formTitle = document.querySelector("#form-title");

    function escapeHtml(value) {
        return String(value).replace(/[&<>"']/g, (character) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[character]);
    }

    async function render() {
        let notes;
        try {
            notes = await FinanceStore.getNotes();
        } catch (error) {
            list.innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
            return;
        }

        if (!notes.length) {
            list.innerHTML = '<p class="empty-state">Nenhuma anotação cadastrada.</p>';
            return;
        }

        list.innerHTML = notes.map((note) => `
            <article class="record-card note-card">
                <div class="record-main">
                    <strong class="record-title">${escapeHtml(note.title)}</strong>
                    <small class="record-meta">${note.date
                        ? `Lembrete · ${FinanceStore.formatDate(note.date)}`
                        : "Anotação pessoal"}</small>
                    <p class="note-content">${escapeHtml(note.content)}</p>
                </div>
                <div class="record-actions">
                    <button class="text-button" type="button" data-edit-note="${escapeHtml(note.id)}">Editar</button>
                    <button class="danger-button" type="button" data-delete-note="${escapeHtml(note.id)}">Excluir</button>
                </div>
            </article>
        `).join("");
    }

    function openForm(note) {
        form.reset();
        message.textContent = "";
        idInput.value = note?.id || "";
        titleInput.value = note?.title || "";
        dateInput.value = note?.date
            ? FinanceStore.formatDate(note.date)
            : "";
        contentInput.value = note?.content || "";
        formTitle.textContent = note ? "Editar anotação" : "Criar anotação";
        panel.hidden = false;
        titleInput.focus();
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
            await FinanceStore.saveNote({
                id: idInput.value || undefined,
                title: titleInput.value,
                content: contentInput.value,
                date: dateInput.value
                    ? FinanceStore.parseDateInput(dateInput.value)
                    : ""
            });
            closeForm();
            await render();
        } catch (error) {
            message.textContent = error.message;
        }
    });

    list.addEventListener("click", async (event) => {
        const editButton = event.target.closest("[data-edit-note]");
        const deleteButton = event.target.closest("[data-delete-note]");

        if (editButton) {
            const notes = await FinanceStore.getNotes();
            const note = notes.find((item) => item.id === editButton.dataset.editNote);
            if (note) openForm(note);
        }

        if (deleteButton && window.confirm("Excluir esta anotação?")) {
            try {
                await FinanceStore.deleteNote(deleteButton.dataset.deleteNote);
                await render();
            } catch (error) {
                window.alert(error.message);
            }
        }
    });

    render();
})();
