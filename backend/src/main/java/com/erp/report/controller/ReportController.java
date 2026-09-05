package com.erp.report.controller;

import com.alibaba.excel.EasyExcel;
import com.erp.common.Result;
import com.erp.report.excel.MonthlyExcelVO;
import com.erp.report.excel.StatementExcelVO;
import com.erp.report.service.ReportService;
import com.erp.report.vo.MonthlyReportVO;
import com.erp.report.vo.StatementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 报表与导出接口。
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "报表与导出")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/statement/{customerId}")
    @Operation(summary = "客户对账单", description = "期末欠款 = 期初 + 本期发货 − 本期回款")
    public Result<StatementVO> statement(@PathVariable Integer customerId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.success(reportService.statement(customerId, from, to));
    }

    @GetMapping("/monthly")
    @Operation(summary = "月度经营报表", description = "订单量、出货量、回款额、完成率 + 近 12 月趋势")
    public Result<MonthlyReportVO> monthly(@RequestParam String month) {
        return Result.success(reportService.monthly(month));
    }

    @GetMapping("/export/statement/{customerId}")
    @Operation(summary = "导出对账单 Excel")
    public void exportStatement(HttpServletResponse response,
                                @PathVariable Integer customerId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws IOException {
        StatementVO vo = reportService.statement(customerId, from, to);
        String fileName = String.format("对账单_%s_%s_%s.xlsx",
                customerId, from.format(DateTimeFormatter.BASIC_ISO_DATE), to.format(DateTimeFormatter.BASIC_ISO_DATE));
        writeExcel(response, fileName, StatementExcelVO.class, reportService.toStatementExcel(vo));
    }

    @GetMapping("/export/monthly")
    @Operation(summary = "导出月度报表 Excel")
    public void exportMonthly(HttpServletResponse response, @RequestParam String month) throws IOException {
        MonthlyReportVO vo = reportService.monthly(month);
        String fileName = String.format("月度报表_%s.xlsx", month.replace("-", ""));
        writeExcel(response, fileName, MonthlyExcelVO.class, reportService.toMonthlyExcel(vo));
    }

    private void writeExcel(HttpServletResponse response, String fileName, Class<?> head, java.util.List<?> data) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String disposition = "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", disposition);
        EasyExcel.write(response.getOutputStream(), head).sheet("Sheet1").doWrite(data);
    }
}
