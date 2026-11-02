package com.meufinanceiro.controller;

import com.meufinanceiro.dto.Requests.NoteBody;
import com.meufinanceiro.model.Note;
import com.meufinanceiro.service.NoteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Note> list(@RequestAttribute("userId") UUID userId) {
        return service.list(userId);
    }

    @PostMapping
    public Note create(@RequestAttribute("userId") UUID userId, @RequestBody NoteBody body) {
        return service.create(userId, body);
    }

    @PutMapping("/{id}")
    public Note update(@RequestAttribute("userId") UUID userId, @PathVariable("id") String id,
                       @RequestBody NoteBody body) {
        return service.update(userId, id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@RequestAttribute("userId") UUID userId, @PathVariable("id") String id) {
        service.delete(userId, id);
        return Map.of("ok", true);
    }
}
