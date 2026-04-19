package com.quiz.main.controller;

import java.util.List;

import com.quiz.main.model.Quiz;
import com.quiz.main.model.User;
import com.quiz.main.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

@Controller
public class MainController {
	

	@Autowired
    QuizService qService;
	@Autowired
	private QuizService quizService;
	Boolean submitted = false;
	

	
	@GetMapping("/")
	public String home() {
		return "index.html";
	}



	@GetMapping("/user/home")
	public ModelAndView userHome(HttpSession session) {
		ModelAndView modelAndView = new ModelAndView();
		User currentUser = (User) session.getAttribute("currentUser");
		if (currentUser != null && "ROLE_USER".equals(currentUser.getRole())) {
			List<Quiz> quizzes = quizService.getAllQuizzes(); // Assuming you have this method in your QuizService
			modelAndView.addObject("quizzes", quizzes);
			modelAndView.setViewName("userHome");
		} else {
			modelAndView.setViewName("redirect:/login"); // Redirect to login if not user
		}
		return modelAndView;
	}


	@GetMapping("/admin/home")
	public ModelAndView adminHome(HttpSession session) {
		ModelAndView modelAndView = new ModelAndView();
		User currentUser = (User) session.getAttribute("currentUser");
		if (currentUser != null && "ROLE_ADMIN".equals(currentUser.getRole())) {
			List<Quiz> quizzes = quizService.getAllQuizzes();
			int totalQuestions = quizzes.stream()
					.mapToInt(quiz -> quiz.getQuestions() == null ? 0 : quiz.getQuestions().size())
					.sum();
			modelAndView.addObject("quizzes", quizzes);
			modelAndView.addObject("quizCount", quizzes.size());
			modelAndView.addObject("questionCount", totalQuestions);
			modelAndView.setViewName("adminHome");
		} else {
			modelAndView.setViewName("redirect:/login"); // Redirect to login if not admin
		}
		return modelAndView;
	}
	@GetMapping("/quizTimeout")
	public String quizTimeout() {
		return "quizTimeout";
	}

}

