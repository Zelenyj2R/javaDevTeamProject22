package webnotes.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import webnotes.model.entity.Note;
import webnotes.model.security.NoteValidator;
import webnotes.model.service.NoteContentFormatService;
import webnotes.model.service.NoteService;
import webnotes.model.service.UserService;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Controller
@RequestMapping(value = {"/note", "/"})
public class NoteController {
    private final NoteService noteService;
    private final UserService userService;
    private final NoteValidator noteValidator;
    private final NoteContentFormatService noteContentFormatService;

    @GetMapping("/list")
    public ModelAndView getListOfNotes(Principal principal, HttpSession session) {
        session.removeAttribute("note");
        int userId = userService.getIdByUsername(principal.getName());
        List<Note> notes = noteService.listAll(userId);
        return new ModelAndView("list")
                .addObject("notes", noteContentFormatService.getFormattedList(notes));
    }

    @GetMapping("/")
    public ModelAndView getListOfNotesFromRoot() {
        return new ModelAndView("redirect:/note/list");
    }

    @PostMapping("/delete")
    public ModelAndView deleteNoteById(@RequestParam String id) {
        noteService.deleteById(id);
        return new ModelAndView("redirect:/note/list");
    }

    @GetMapping("/edit")
    public ModelAndView showEditPage(@RequestParam("id") String id, HttpSession session) {
        Note sessionNote = (Note) session.getAttribute("note");
        if (sessionNote == null) {
            Optional<Note> note = noteService.getById(id);
            return note.map(value -> new ModelAndView("edit")
                            .addObject("note", value))
                    .orElseGet(() -> new ModelAndView("redirect:/note/list"));
        }
        return new ModelAndView("edit")
                .addObject("note", sessionNote);
    }

    @PostMapping("/edit")
    public ModelAndView editNote(@ModelAttribute Note note, HttpSession session)  {
        if (!noteService.isNoteExist(note.getId())) {
            return new ModelAndView("redirect:/note/list");
        }
        List<String> errorMessageList = noteValidator.isNoteValid(note);
        if (errorMessageList.isEmpty()) {
            noteService.update(note);
            return new ModelAndView("redirect:/note/list");
        }
        session.setAttribute("note", note);
        return new ModelAndView("error")
                .addObject("backLink", "/note/edit?id=" + note.getId())
                .addObject("errMes", errorMessageList);
    }

    @GetMapping("/create")
    public ModelAndView showCreatePage(HttpSession session) {
        Note note = (Note) session.getAttribute("note");
        if (note == null) {
            return new ModelAndView("create")
                    .addObject("isEmpty", true);
        }
        return new ModelAndView("create")
                .addObject("note", note)
                .addObject("isEmpty", false);
    }

    @PostMapping("/create")
    public ModelAndView createNote(@ModelAttribute Note note, Principal principal, HttpSession session)  {
        List<String> errorMessageList = noteValidator.isNoteValid(note);
        if (errorMessageList.isEmpty()) {
            int userId = userService.getIdByUsername(principal.getName());
            noteService.add(note, userId);
            return new ModelAndView("redirect:/note/list");
        }
        session.setAttribute("note", note);
        return new ModelAndView("error")
                .addObject("backLink", "/note/create")
                .addObject("errMes", errorMessageList);
    }

    @GetMapping("/share/{id}")
    public ModelAndView showSharePage(@PathVariable String id) {
        Optional<Note> note = noteService.getById(id);
        if (note.isPresent() && "public".equalsIgnoreCase(note.get().getAccess())) {
            return new ModelAndView("share")
                    .addObject("note", noteContentFormatService.getFormattedNote(note.get()))
                    .addObject("isPublic", true);
        }
        return new ModelAndView("share")
                .addObject("message", "This Note is private or doesn't exist")
                .addObject("isPublic", false);
    }
}
