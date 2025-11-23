package welfen.welfen_api.WelfenAPI;

import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.springframework.web.bind.annotation.*;

import welfen.welfen_api.WelfenAPI.model.User;

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
		String role = "user";
		File modsFile = new File("mods.txt");
		if(!modsFile.exists()) {
			try {
				modsFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		List<String> mods = new ArrayList<String>();
		
		try {
			FileReader fr = new FileReader("mods.txt");
			BufferedReader br = new BufferedReader(fr);
			
			try {
				String line;
				while ((line = br.readLine()) != null) {
					mods.add(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if (mods.contains(username)) {
			role = "mod";
		}
		
		userService.register(username, password, role);
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
	
    @GetMapping("/me")
    public Object getMe(@RequestHeader("Authorization") String token) {
        String username = jwtService.getUsernameFromToken(token);
        String role = userService.getRole(username);

        return new Object() {
            public String user = username;
            public String userRole = role;
        };
    }
    
    @GetMapping("/all")
    public List<User> getAllUsers(@RequestHeader("Authorization") String token) {
        String username = jwtService.getUsernameFromToken(token);

        // Berechtigung prüfen
        String role = userService.getRole(username);
        if (!role.equals("mod")) {
            throw new RuntimeException("NO_PERMISSION");
        }

        return userService.getAllUsers();
    }
    
    @PostMapping("/setrole")
    public String setRole(@RequestHeader("Authorization") String token,
                          @RequestParam String targetUser,
                          @RequestParam String role) {

        if (!role.equals("user") && !role.equals("mod")) {
            throw new RuntimeException("INVALID_ROLE");
        }

        String requester = jwtService.getUsernameFromToken(token);
        String requesterRole = userService.getRole(requester);

        if (!requesterRole.equals("mod")) {
            throw new RuntimeException("NO_PERMISSION");
        }

        userService.setRole(targetUser, role);
        return "Ok";
    }
}
