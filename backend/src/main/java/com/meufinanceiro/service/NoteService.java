package com.meufinanceiro.service;

import com.meufinanceiro.dto.Requests.NoteBody;
import com.meufinanceiro.model.Note;
import com.meufinanceiro.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NoteService {
    private static final String REQUIRED = "Informe o título e o conteúdo da anotação.";
    private final NoteRepository repository;

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    public List<Note> list(UUID userId) {
        return repository.findAll(userId);
    }

    public Note create(UUID userId, NoteBody b) {
        return repository.create(userId, Validate.text(b.title(), REQUIRED, 200),
                Validate.text(b.content(), REQUIRED, 5000), Validate.date(b.date(), false));
    }

    public Note update(UUID userId, String id, NoteBody b) {
        return repository.update(userId, Validate.id(id), Validate.text(b.title(), REQUIRED, 200),
                        Validate.text(b.content(), REQUIRED, 5000), Validate.date(b.date(), false))
                .orElseThrow(Validate::notFound);
    }

    public void delete(UUID userId, String id) {
        if (!repository.delete(userId, Validate.id(id))) throw Validate.notFound();
    }
}
