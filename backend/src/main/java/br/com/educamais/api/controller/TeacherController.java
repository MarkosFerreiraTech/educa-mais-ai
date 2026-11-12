package br.com.educamais.api.controller;

import br.com.educamais.api.dto.DashboardResponse;
import br.com.educamais.api.dto.TeacherReportResponse;
import br.com.educamais.api.mapper.ApiMapper;
import br.com.educamais.domain.exception.ResourceNotFoundException;
import br.com.educamais.service.TeacherDashboardService;
import br.com.educamais.service.TeacherReportService;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Area do professor: dashboard e relatorios (RF33, RF34 / UC07, UC10). */
@RestController
@RequestMapping("/api/v1/teacher")
public class TeacherController {

    private final TeacherDashboardService dashboardService;
    private final TeacherReportService reportService;

    public TeacherController(TeacherDashboardService dashboardService, TeacherReportService reportService) {
        this.dashboardService = dashboardService;
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam UUID classroomId) {
        return ApiMapper.toDashboard(dashboardService.dashboard(classroomId));
    }

    @GetMapping("/reports/{missionId}")
    public TeacherReportResponse report(@PathVariable UUID missionId) {
        return reportService.byMission(missionId)
                .map(ApiMapper::toReport)
                .orElseThrow(() -> new ResourceNotFoundException("Relatorio", missionId));
    }
}
