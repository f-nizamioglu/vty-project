package com.vty.vty.controller;

import com.vty.vty.entity.Reservation;
import com.vty.vty.entity.Review;
import com.vty.vty.entity.Tool;
import com.vty.vty.entity.ToolPerformanceView;
import com.vty.vty.entity.User;
import com.vty.vty.model.Role;
import com.vty.vty.repository.ReservationRepository;
import com.vty.vty.repository.ReviewRepository;
import com.vty.vty.repository.ToolRepository;
import com.vty.vty.repository.ToolPerformanceViewRepository;
import com.vty.vty.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ToolRepository toolRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ToolPerformanceViewRepository toolPerformanceViewRepository;

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(@RequestParam(required = false) Long minReservations,
                           @RequestParam(required = false) Double minReview,
                           @RequestParam(required = false) Long minUserReservations,
                           @RequestParam(required = false) LocalDate startDate,
                           @RequestParam(required = false) LocalDate endDate,
                           @RequestParam(required = false) Integer daysPast,
                           Authentication authentication, 
                           Model model) {
        User user = (User) authentication.getPrincipal();
        
        if (user.getRole() == Role.ADMIN) {
            return adminDashboard(minReservations, minReview, minUserReservations, model, authentication);
        } else {
            return userDashboard(user, model, startDate, endDate, daysPast);
        }
    }

    @Transactional(readOnly = true)
    private String userDashboard(User user, Model model, LocalDate startDate, LocalDate endDate, Integer daysPast) {
        List<Tool> userTools = toolRepository.findByUserId(user.getId());
        List<Tool> availableTools = toolRepository.findAllExceptUserId(user.getId());
        
        List<Object[]> userReservationsData = reservationRepository.getUserReservationsWithDetails(
            user.getId(), startDate, endDate);
        
        List<Map<String, Object>> userReservationsWithDetails = userReservationsData.stream().map(data -> {
            Map<String, Object> reservationMap = new HashMap<>();
            reservationMap.put("reservationId", data[0]);
            reservationMap.put("toolId", data[1]);
            reservationMap.put("toolName", data[2]);
            reservationMap.put("reservationDate", data[3]);
            reservationMap.put("reviewPoint", data[4]);
            reservationMap.put("hasReview", data[5]);
            reservationMap.put("ownerName", data[6]);
            reservationMap.put("ownerEmail", data[7]);
            return reservationMap;
        }).collect(Collectors.toList());
        
        Integer daysPastParam = (daysPast != null && daysPast > 0) ? daysPast : 0;
        List<Object[]> pendingReviewsData = reservationRepository.getPendingReviewReservations(
            user.getId(), daysPastParam);
        
        List<Map<String, Object>> pendingReviews = pendingReviewsData.stream().map(data -> {
            Map<String, Object> pendingMap = new HashMap<>();
            pendingMap.put("reservationId", data[0]);
            pendingMap.put("toolId", data[1]);
            pendingMap.put("toolName", data[2]);
            pendingMap.put("reservationDate", data[3]);
            pendingMap.put("daysSinceReservation", data[4]);
            pendingMap.put("ownerName", data[5]);
            pendingMap.put("ownerEmail", data[6]);
            pendingMap.put("ownerPhone", data[7]);
            return pendingMap;
        }).collect(Collectors.toList());
        
        List<Reservation> userReservations = reservationRepository.findByUserId(user.getId());
        
        model.addAttribute("tools", userTools);
        model.addAttribute("availableTools", availableTools);
        model.addAttribute("reservations", userReservations);
        model.addAttribute("userReservationsWithDetails", userReservationsWithDetails);
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("daysPast", daysPastParam);
        model.addAttribute("currentDate", LocalDate.now());
        model.addAttribute("currentUser", user);
        
        return "dashboard";
    }

    @Transactional(readOnly = true)
    private String adminDashboard(Long minReservations, Double minReview, Long minUserReservations, Model model, Authentication authentication) {
        User admin = (User) authentication.getPrincipal();
        List<Tool> allTools = toolRepository.findAll();
        List<Reservation> allReservations = reservationRepository.findAll();
        List<User> allUsers = userRepository.findAll();
        List<ToolPerformanceView> toolPerformanceViews = toolPerformanceViewRepository.findAllOrderByReservationsDesc();
        
        List<ToolPerformanceView> topPerformers = null;
        if (minReservations != null && minReservations > 0 || minReview != null && minReview > 0) {
            Long minRes = minReservations != null ? minReservations : 0L;
            Double minRev = minReview != null ? minReview : 0.0;
            topPerformers = toolPerformanceViewRepository.findTopPerformersByReservationsOrReviews(minRes, minRev);
        }
        
        List<Object[]> userReservationStats = null;
        Map<Long, User> userMap = allUsers.stream().collect(Collectors.toMap(User::getId, user -> user));
        List<Map<String, Object>> userReservationStatsWithDetails = null;
        
        if (minUserReservations != null && minUserReservations > 0) {
            userReservationStats = reservationRepository.findUsersWithMinReservations(minUserReservations);
            userReservationStatsWithDetails = userReservationStats.stream().map(stat -> {
                Map<String, Object> statMap = new HashMap<>();
                Long userId = ((Number) stat[0]).longValue();
                Long reservationCount = ((Number) stat[1]).longValue();
                statMap.put("userId", userId);
                statMap.put("reservationCount", reservationCount);
                User user = userMap.get(userId);
                if (user != null) {
                    statMap.put("fullName", user.getFullName());
                    statMap.put("email", user.getEmail());
                    statMap.put("phoneNumber", user.getPhoneNumber());
                } else {
                    statMap.put("fullName", "N/A");
                    statMap.put("email", "N/A");
                    statMap.put("phoneNumber", "N/A");
                }
                return statMap;
            }).collect(Collectors.toList());
        }
        
        model.addAttribute("tools", allTools);
        model.addAttribute("reservations", allReservations);
        model.addAttribute("users", allUsers);
        model.addAttribute("toolPerformanceViews", toolPerformanceViews);
        model.addAttribute("topPerformers", topPerformers);
        model.addAttribute("userReservationStats", userReservationStatsWithDetails);
        model.addAttribute("minReservations", minReservations);
        model.addAttribute("minReview", minReview);
        model.addAttribute("minUserReservations", minUserReservations);
        model.addAttribute("currentDate", LocalDate.now());
        model.addAttribute("currentUser", admin);
        
        return "admin-dashboard";
    }
    
    @GetMapping("/tools/{id}/stats")
    @Transactional(readOnly = true)
    public String getToolStats(@PathVariable Long id, 
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        
        Tool tool = toolRepository.findById(id)
                .orElse(null);
        
        if (tool == null) {
            redirectAttributes.addFlashAttribute("error", "Alet bulunamadı!");
            return "redirect:/dashboard";
        }
        
        List<Object[]> statsData = toolRepository.getToolReservationStats(id);
        
        if (statsData != null && !statsData.isEmpty()) {
            Object[] stats = statsData.get(0);
            Map<String, Object> toolStats = new HashMap<>();
            toolStats.put("toolId", stats[0]);
            toolStats.put("toolName", stats[1]);
            toolStats.put("totalReservations", stats[2]);
            toolStats.put("futureReservations", stats[3]);
            toolStats.put("pastReservations", stats[4]);
            toolStats.put("avgReviewPoint", stats[5]);
            toolStats.put("totalReviews", stats[6]);
            toolStats.put("ownerId", stats[7]);
            toolStats.put("ownerName", stats[8]);
            toolStats.put("ownerEmail", stats[9]);
            toolStats.put("firstReservationDate", stats[10]);
            toolStats.put("lastReservationDate", stats[11]);
            
            model.addAttribute("toolStats", toolStats);
            model.addAttribute("tool", tool);
            model.addAttribute("currentUser", user);
            model.addAttribute("currentDate", LocalDate.now());
        } else {
            redirectAttributes.addFlashAttribute("error", "Alet istatistikleri bulunamadı!");
            return "redirect:/dashboard";
        }
        
        return "tool-stats";
    }

    @PostMapping("/review")
    @Transactional
    public String submitReview(@RequestParam Long reservationId, 
                              @RequestParam Long reviewPoint,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Rezervasyon bulunamadı"));
        
        if (!reservation.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bu rezervasyon size ait değil!");
            return "redirect:/dashboard";
        }
        
        if (reservation.getReview() != null) {
            redirectAttributes.addFlashAttribute("error", "Bu rezervasyon zaten değerlendirilmiş!");
            return "redirect:/dashboard";
        }
        
        if (reviewRepository.findByReservationId(reservationId).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Bu rezervasyon zaten değerlendirilmiş!");
            return "redirect:/dashboard";
        }
        
        Review review = new Review();
        review.setReviewPoint(reviewPoint);
        review.setReservation(reservation);
        review = reviewRepository.save(review);
        
        reservation.setReview(review);
        reservationRepository.save(reservation);
        
        toolRepository.flush();
        Tool tool = toolRepository.findById(reservation.getTool().getId())
                .orElse(reservation.getTool());
        
        String successMessage = String.format(
            "Değerlendirme başarıyla kaydedildi! Aletin güncel ortalama puanı: %.2f",
            tool.getAvgReview() != null ? tool.getAvgReview() : 0.0
        );
        redirectAttributes.addFlashAttribute("success", successMessage);
        return "redirect:/dashboard";
    }

    @PostMapping("/admin/users")
    @Transactional
    public String addUser(@RequestParam String fullName,
                         @RequestParam String email,
                         @RequestParam String phoneNumber,
                         @RequestParam String role,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User admin = (User) authentication.getPrincipal();
        
        if (admin.getRole() != Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Yetkiniz yok!");
            return "redirect:/dashboard";
        }
        
        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Bu email zaten kullanılıyor!");
            return "redirect:/dashboard";
        }
        
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhoneNumber(phoneNumber);
        newUser.setRole(Role.valueOf(role));
        
        userRepository.save(newUser);
        
        redirectAttributes.addFlashAttribute("success", "Kullanıcı başarıyla eklendi!");
        return "redirect:/dashboard";
    }

    @PostMapping("/admin/users/{id}/update")
    @Transactional
    public String updateUser(@PathVariable Long id,
                             @RequestParam String fullName,
                             @RequestParam String email,
                             @RequestParam String phoneNumber,
                             @RequestParam String role,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User admin = (User) authentication.getPrincipal();
        
        if (admin.getRole() != Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Yetkiniz yok!");
            return "redirect:/dashboard";
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        if (!user.getEmail().equals(email)) {
            if (userRepository.findByEmail(email).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Bu email zaten kullanılıyor!");
                return "redirect:/dashboard";
            }
        }
        
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setRole(Role.valueOf(role));
        
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "Kullanıcı bilgileri başarıyla güncellendi!");
        return "redirect:/dashboard";
    }

    @PostMapping("/tools")
    @Transactional
    public String addTool(@RequestParam String toolName,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        
        if (toolName == null || toolName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Alet adı boş olamaz!");
            return "redirect:/dashboard";
        }
        
        Tool tool = new Tool();
        tool.setToolName(toolName.trim());
        tool.setUser(user);
        
        toolRepository.save(tool);
        
        redirectAttributes.addFlashAttribute("success", "Alet başarıyla eklendi!");
        return "redirect:/dashboard";
    }

    @PostMapping("/reservations")
    @Transactional
    public String createReservation(@RequestParam Long toolId,
                                   @RequestParam LocalDate reservationDate,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        
        if (reservationDate == null) {
            redirectAttributes.addFlashAttribute("error", "Rezervasyon tarihi seçilmelidir!");
            return "redirect:/dashboard";
        }
        
        if (reservationDate.isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("error", "Geçmiş tarih seçilemez!");
            return "redirect:/dashboard";
        }
        
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new RuntimeException("Alet bulunamadı"));
        
        if (tool.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Kendi aletinizi rezerve edemezsiniz!");
            return "redirect:/dashboard";
        }
        
        List<Reservation> existingReservations = reservationRepository.findByToolIdAndDate(toolId, reservationDate);
        if (!existingReservations.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bu tarih için bu alet zaten rezerve edilmiş!");
            return "redirect:/dashboard";
        }
        
        try {
            Reservation reservation = new Reservation();
            reservation.setTool(tool);
            reservation.setUser(user);
            reservation.setReservationDate(reservationDate);
            
            reservationRepository.save(reservation);
            
            redirectAttributes.addFlashAttribute("success", "Rezervasyon başarıyla oluşturuldu!");
            return "redirect:/dashboard";
        } catch (DataIntegrityViolationException | org.hibernate.exception.ConstraintViolationException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Geçmiş tarihli rezervasyon")) {
                redirectAttributes.addFlashAttribute("error", "Geçmiş tarihli rezervasyon oluşturulamaz!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Rezervasyon oluşturulurken bir hata oluştu!");
            }
            return "redirect:/dashboard";
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Geçmiş tarihli rezervasyon")) {
                redirectAttributes.addFlashAttribute("error", "Geçmiş tarihli rezervasyon oluşturulamaz!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Rezervasyon oluşturulurken bir hata oluştu: " + e.getMessage());
            }
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/reservations/{id}/cancel")
    @Transactional
    public String cancelReservation(@PathVariable Long id,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rezervasyon bulunamadı"));
        
        if (!reservation.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Bu rezervasyonu iptal etme yetkiniz yok!");
            return "redirect:/dashboard";
        }
        
        if (reservation.getReservationDate().isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("error", "Geçmiş tarihli rezervasyonlar iptal edilemez!");
            return "redirect:/dashboard";
        }
        
        if (reservation.getReview() != null) {
            reviewRepository.delete(reservation.getReview());
        }
        
        reservationRepository.delete(reservation);
        
        redirectAttributes.addFlashAttribute("success", "Rezervasyon başarıyla iptal edildi!");
        return "redirect:/dashboard";
    }

    @PostMapping("/reservations/{id}/update")
    @Transactional
    public String updateReservation(@PathVariable Long id,
                                     @RequestParam LocalDate reservationDate,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rezervasyon bulunamadı"));
        
        if (!reservation.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Bu rezervasyonu güncelleme yetkiniz yok!");
            return "redirect:/dashboard";
        }
        
        if (reservationDate == null) {
            redirectAttributes.addFlashAttribute("error", "Rezervasyon tarihi seçilmelidir!");
            return "redirect:/dashboard";
        }
        
        if (reservationDate.isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("error", "Geçmiş tarih seçilemez!");
            return "redirect:/dashboard";
        }
        
        List<Reservation> existingReservations = reservationRepository.findByToolIdAndDate(
                reservation.getTool().getId(), reservationDate);
        if (!existingReservations.isEmpty() && 
            existingReservations.stream().anyMatch(r -> !r.getId().equals(id))) {
            redirectAttributes.addFlashAttribute("error", "Bu tarih için bu alet zaten rezerve edilmiş!");
            return "redirect:/dashboard";
        }
        
        try {
            reservation.setReservationDate(reservationDate);
            reservationRepository.save(reservation);
            
            redirectAttributes.addFlashAttribute("success", "Rezervasyon tarihi başarıyla güncellendi!");
            return "redirect:/dashboard";
        } catch (DataIntegrityViolationException | org.hibernate.exception.ConstraintViolationException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Geçmiş tarihli rezervasyon")) {
                redirectAttributes.addFlashAttribute("error", "Geçmiş tarihli rezervasyon oluşturulamaz veya güncellenemez!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Rezervasyon güncellenirken bir hata oluştu!");
            }
            return "redirect:/dashboard";
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Geçmiş tarihli rezervasyon")) {
                redirectAttributes.addFlashAttribute("error", "Geçmiş tarihli rezervasyon oluşturulamaz veya güncellenemez!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Rezervasyon güncellenirken bir hata oluştu: " + e.getMessage());
            }
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/tools/{id}/delete")
    @Transactional
    public String deleteTool(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User user = (User) authentication.getPrincipal();
        
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alet bulunamadı"));
        
        if (!tool.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Bu aleti silme yetkiniz yok!");
            return "redirect:/dashboard";
        }
        
        List<Reservation> toolReservations = reservationRepository.findByToolId(id);
        
        for (Reservation reservation : toolReservations) {
            if (reservation.getReview() != null) {
                reviewRepository.delete(reservation.getReview());
            }
            reservationRepository.delete(reservation);
        }
        
        toolRepository.delete(tool);
        
        redirectAttributes.addFlashAttribute("success", "Alet başarıyla silindi!");
        return "redirect:/dashboard";
    }
}

