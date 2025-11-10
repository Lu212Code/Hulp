package welfen.welfen_api.WelfenAPI;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WelfenApiController {

    @GetMapping("/")
    @ResponseBody
    public String api() {
        return "Welfen-API is running.";
    }
    
    @GetMapping("/info")
    @ResponseBody
    public String info() {
    	return "Welfen-API Ver. 1.0";
    }
}
