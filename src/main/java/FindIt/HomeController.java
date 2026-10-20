
package FindIt;

import FindIt.model.Item;
import FindIt.service.ItemService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class HomeController
{
    private final ItemService itemService;

    private final Path uploadPath =
            Paths.get("uploads").toAbsolutePath().normalize();

    public HomeController(ItemService itemService) throws IOException
    {
        this.itemService = itemService;
        Files.createDirectories(uploadPath);
    }

    @GetMapping("/")
    public String home(Model model)
    {
        addDashboardData(model);
        model.addAttribute("item", new Item());
        return "index";
    }

    private void addDashboardData(Model model)
    {
        model.addAttribute("items", itemService.getAllItems());
        model.addAttribute("totalCount", itemService.getTotalCount());
        model.addAttribute("lostCount", itemService.getLostCount());
        model.addAttribute("foundCount", itemService.getFoundCount());
        model.addAttribute("returnedCount", itemService.getReturnedCount());
    }

    @PostMapping("/report")
    public String reportItem(
            @Valid @ModelAttribute("item") Item item,
            BindingResult result,
            @RequestParam(value = "imageFile", required = false)
            MultipartFile imageFile,
            Model model,
            RedirectAttributes redirectAttributes)
    {
        if (item.getStatus() == null ||
            !(item.getStatus().equals("LOST") ||
              item.getStatus().equals("FOUND")))
        {
            result.rejectValue(
                    "status", "invalid.status",
                    "Choose Lost or Found"
            );
        }

        if (result.hasErrors())
        {
            addDashboardData(model);
            return "index";
        }

        if (imageFile != null && !imageFile.isEmpty())
        {
            if (imageFile.getSize() > 5 * 1024 * 1024)
            {
                addDashboardData(model);
                model.addAttribute(
                        "uploadError",
                        "Image size must be 5 MB or less."
                );
                return "index";
            }

            String contentType = imageFile.getContentType();

            if (contentType == null ||
                !contentType.matches(
                        "image/(jpeg|png|gif|webp)"))
            {
                addDashboardData(model);
                model.addAttribute(
                        "uploadError",
                        "Upload a JPG, PNG, GIF or WebP image."
                );
                return "index";
            }

            String originalName = imageFile.getOriginalFilename();
            String extension = "";

            if (originalName != null &&
                originalName.lastIndexOf('.') >= 0)
            {
                extension = originalName.substring(
                        originalName.lastIndexOf('.'))
                        .toLowerCase();
            }

            if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)"))
            {
                addDashboardData(model);
                model.addAttribute(
                        "uploadError",
                        "The image file extension is not supported."
                );
                return "index";
            }

            String fileName = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(fileName).normalize();

            if (!filePath.startsWith(uploadPath))
            {
                addDashboardData(model);
                model.addAttribute(
                        "uploadError",
                        "Invalid image filename."
                );
                return "index";
            }

            try
            {
                byte[] fileBytes = imageFile.getBytes();

                if (!isAllowedImage(fileBytes, contentType))
                {
                    addDashboardData(model);
                    model.addAttribute(
                            "uploadError",
                            "The uploaded file does not appear to be a valid image."
                    );
                    return "index";
                }

                Files.write(filePath, fileBytes);
                item.setImageName(fileName);
            }
            catch (IOException e)
            {
                addDashboardData(model);
                model.addAttribute(
                        "uploadError",
                        "Could not save the image. Please try again."
                );
                return "index";
            }
        }

        itemService.saveItem(item);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your report was submitted successfully!"
        );

        return "redirect:/";
    }

    private boolean isAllowedImage(byte[] bytes, String contentType)
    {
        if (bytes.length < 4)
        {
            return false;
        }

        boolean jpeg = bytes.length >= 3 &&
                (bytes[0] & 0xFF) == 0xFF &&
                (bytes[1] & 0xFF) == 0xD8 &&
                (bytes[2] & 0xFF) == 0xFF;

        boolean png = bytes.length >= 8 &&
                (bytes[0] & 0xFF) == 0x89 &&
                bytes[1] == 0x50 &&
                bytes[2] == 0x4E &&
                bytes[3] == 0x47 &&
                bytes[4] == 0x0D &&
                bytes[5] == 0x0A &&
                bytes[6] == 0x1A &&
                bytes[7] == 0x0A;

        boolean gif = bytes.length >= 6 &&
                bytes[0] == 'G' &&
                bytes[1] == 'I' &&
                bytes[2] == 'F' &&
                bytes[3] == '8' &&
                (bytes[4] == '7' || bytes[4] == '9') &&
                bytes[5] == 'a';

        boolean webp = bytes.length >= 12 &&
                bytes[0] == 'R' &&
                bytes[1] == 'I' &&
                bytes[2] == 'F' &&
                bytes[3] == 'F' &&
                bytes[8] == 'W' &&
                bytes[9] == 'E' &&
                bytes[10] == 'B' &&
                bytes[11] == 'P';

        return (contentType.equals("image/jpeg") && jpeg) ||
               (contentType.equals("image/png") && png) ||
               (contentType.equals("image/gif") && gif) ||
               (contentType.equals("image/webp") && webp);
    }

    @PostMapping("/items/{id}/returned")
    public String markReturned(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes)
    {
        if (itemService.markReturned(id))
        {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Item marked as returned!"
            );
        }
        else
        {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Item not found."
            );
        }

        return "redirect:/";
    }
}
