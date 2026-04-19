package com.quiz.main.controller;

import com.quiz.main.model.Quiz;
import com.quiz.main.model.QuizResult;
import com.quiz.main.model.QuizSubmission;
import com.quiz.main.model.User;
import com.quiz.main.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequestMapping("/quizzes")
public class UserQuizController {

    @Autowired
    private QuizService quizService;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    @Value("${RAILWAY_PUBLIC_DOMAIN:}")
    private String railwayPublicDomain;

    @GetMapping
    public String listQuizzes(Model model) {
        model.addAttribute("quizzes", quizService.getAllQuizzes());
        return "userQuizzesList";
    }

    @GetMapping("/{id}")
    public String takeQuiz(@PathVariable Long id, Model model, HttpSession session) {
        Quiz quiz = quizService.getQuizWithQuestionsById(id);
        if (quiz == null) {
            return "redirect:/quiz-not-found"; // or handle the error appropriately
        }

        // Store the start time in the session to calculate the duration on submission
        long startTime = System.currentTimeMillis();
        session.setAttribute("startTime", startTime);

        // Pass the quiz, its questions, and the duration to the view
        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", quiz.getQuestions());
        model.addAttribute("duration", quiz.getDuration()); // Assuming quiz.getDuration() gives you the duration in minutes
        model.addAttribute("quizSubmission", new QuizSubmission());

        return "takeQuiz";
    }

    @PostMapping("/{quizId}/submit")
    public String submitQuizAnswers(@PathVariable Long quizId, @ModelAttribute QuizSubmission submission, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Quiz quiz = quizService.getQuizWithQuestionsById(quizId);
        Map<Long, Character> submittedAnswers = submission.getAnswers() == null
                ? Collections.emptyMap()
                : submission.getAnswers();
        int score = quizService.calculateScore(quiz, submittedAnswers);

        QuizResult quizResult = quizService.saveQuizResult(currentUser, quiz, score, submittedAnswers);
        return "redirect:/quizzes/results/" + quizResult.getResultToken();
    }

    @RequestMapping(value = "/results/{resultToken}", method = {RequestMethod.GET, RequestMethod.POST})
    public String showQuizResult(@PathVariable String resultToken, Model model, HttpServletRequest request) {
        QuizResult quizResult = quizService.getQuizResultByToken(resultToken);
        Quiz quiz = quizService.getQuizWithQuestionsById(quizResult.getQuiz().getId());
        Map<Long, Character> submittedAnswers = quizService.parseSubmittedAnswers(quizResult.getSubmittedAnswers());
        Map<Long, Character> correctAnswers = quizService.getCorrectAnswers(quiz.getId());
        String publicResultUrl = buildPublicResultUrl(resultToken, request);

        model.addAttribute("quiz", quiz);
        model.addAttribute("score", quizResult.getScore());
        model.addAttribute("totalQuestions", quizResult.getTotalQuestions());
        model.addAttribute("submittedAt", quizResult.getSubmittedAt());
        model.addAttribute("correctAnswers", correctAnswers);
        model.addAttribute("submittedAnswers", submittedAnswers);
        model.addAttribute("resultToken", resultToken);
        model.addAttribute("resultUrl", publicResultUrl);
        model.addAttribute("sharePage", false);

        return "quizResults";
    }

    @RequestMapping(value = "/results/share/{resultToken}", method = {RequestMethod.GET, RequestMethod.POST})
    public String showSharedQuizResult(@PathVariable String resultToken, Model model, HttpServletRequest request) {
        QuizResult quizResult = quizService.getQuizResultByToken(resultToken);
        Quiz quiz = quizService.getQuizWithQuestionsById(quizResult.getQuiz().getId());
        Map<Long, Character> submittedAnswers = quizService.parseSubmittedAnswers(quizResult.getSubmittedAnswers());
        Map<Long, Character> correctAnswers = quizService.getCorrectAnswers(quiz.getId());

        model.addAttribute("quiz", quiz);
        model.addAttribute("score", quizResult.getScore());
        model.addAttribute("totalQuestions", quizResult.getTotalQuestions());
        model.addAttribute("submittedAt", quizResult.getSubmittedAt());
        model.addAttribute("correctAnswers", correctAnswers);
        model.addAttribute("submittedAnswers", submittedAnswers);
        model.addAttribute("resultToken", resultToken);
        model.addAttribute("resultUrl", buildPublicResultUrl(resultToken, request));
        model.addAttribute("sharePage", true);

        return "quizResults";
    }

    @GetMapping("/results/{resultToken}/qr")
    @ResponseBody
    public ResponseEntity<byte[]> generateResultQrCode(@PathVariable String resultToken, HttpServletRequest request) {
        String resultUrl = buildPublicResultUrl(resultToken, request);
        byte[] qrCode = quizService.generateQrCode(resultUrl, 220, 220);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"quiz-result-qr.png\"")
                .body(qrCode);
    }

    private String buildPublicResultUrl(String resultToken, HttpServletRequest request) {
        String path = "/quizzes/results/share/" + resultToken;
        if (publicBaseUrl != null && !publicBaseUrl.trim().isEmpty()) {
            return publicBaseUrl.replaceAll("/+$", "") + path;
        }

        if (railwayPublicDomain != null && !railwayPublicDomain.trim().isEmpty()) {
            return "https://" + railwayPublicDomain.replaceAll("^https?://", "").replaceAll("/+$", "") + path;
        }

        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(path)
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
