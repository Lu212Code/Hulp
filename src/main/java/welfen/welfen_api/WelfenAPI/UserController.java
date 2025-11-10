package welfen.welfen_api.WelfenAPI;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	private final JwtService jwtService;

	public UserController(UserService userService, JwtService jwtService) {
		this.userService = userService;
		this.jwtService = jwtService;
	}

	@PostMapping("/register")
	public String register(@RequestParam String username, @RequestParam String password) {
		userService.register(username, password);
		return "Ok";
	}

	@PostMapping("/login")
	public String login(@RequestParam String username, @RequestParam String password) {
		boolean success = userService.login(username, password);
		if (!success)
			return "Error";
		return jwtService.generateToken(username); // Token zurückgeben
	}

	@PostMapping("/changepassword")
	public String changePassword(@RequestParam String username, @RequestParam String newPassword) {
		userService.changePassword(username, newPassword);
		return "Ok";
	}

	@DeleteMapping("/delete")
	public String deleteUser(@RequestParam String username) {
		userService.deleteUser(username);
		return "Ok";
	}
}
