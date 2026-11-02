(function () {
    function formatInput(value) {
        const digits = value.replace(/\D/g, "").slice(0, 8);
        const day = digits.slice(0, 2);

        if (digits.length < 3) {
            return digits.length === 2 ? `${day}/` : day;
        }

        const month = digits.slice(2, 4);
        if (digits.length < 4) return `${day}/${month}`;
        if (digits.length === 4) return `${day}/${month}/`;

        return `${day}/${month}/${digits.slice(4)}`;
    }

    function getCursorPosition(value, digitCount) {
        let position = 0;
        let digitsFound = 0;

        while (position < value.length && digitsFound < digitCount) {
            if (/\d/.test(value[position])) digitsFound++;
            position++;
        }

        if (value[position] === "/") position++;
        return position;
    }

    function removeDigit(input, digitIndex) {
        const digits = input.value.replace(/\D/g, "");
        const updatedDigits = digits.slice(0, digitIndex) + digits.slice(digitIndex + 1);

        input.value = formatInput(updatedDigits);
        const cursor = getCursorPosition(input.value, digitIndex);
        input.setSelectionRange(cursor, cursor);
    }

    document.querySelectorAll("[data-date-input]").forEach((input) => {
        input.addEventListener("keydown", (event) => {
            if (input.selectionStart !== input.selectionEnd) return;

            const position = input.selectionStart;
            let digitIndex;

            if (event.key === "Backspace" && input.value[position - 1] === "/") {
                digitIndex = input.value
                    .slice(0, position - 1)
                    .replace(/\D/g, "").length - 1;
            } else if (event.key === "Delete" && input.value[position] === "/") {
                digitIndex = input.value
                    .slice(0, position)
                    .replace(/\D/g, "").length;
            } else {
                return;
            }

            const digitCount = input.value.replace(/\D/g, "").length;
            if (digitIndex < 0 || digitIndex >= digitCount) return;

            event.preventDefault();
            removeDigit(input, digitIndex);
        });

        input.addEventListener("input", () => {
            const oldCursor = input.selectionStart ?? input.value.length;
            const digitsBeforeCursor = input.value
                .slice(0, oldCursor)
                .replace(/\D/g, "").length;
            const formattedValue = formatInput(input.value);

            input.value = formattedValue;
            const newCursor = Math.min(
                getCursorPosition(formattedValue, digitsBeforeCursor),
                formattedValue.length
            );
            input.setSelectionRange(newCursor, newCursor);
        });
    });
})();
