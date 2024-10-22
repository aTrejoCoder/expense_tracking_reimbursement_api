package alexisTrejo.expenses.tracking.api.Controller;

import alexisTrejo.expenses.tracking.api.DTOs.Expenses.ExpenseDTO;
import alexisTrejo.expenses.tracking.api.DTOs.Expenses.ExpenseRejectDTO;
import alexisTrejo.expenses.tracking.api.Middleware.JWTSecurity;
import alexisTrejo.expenses.tracking.api.Models.enums.ExpenseStatus;
import alexisTrejo.expenses.tracking.api.Service.Interfaces.ExpenseService;
import alexisTrejo.expenses.tracking.api.Service.Interfaces.NotificationService;
import alexisTrejo.expenses.tracking.api.Utils.ResponseWrapper;
import alexisTrejo.expenses.tracking.api.Utils.Result;
import alexisTrejo.expenses.tracking.api.Utils.Summary.ExpenseSummary;
import alexisTrejo.expenses.tracking.api.Utils.Validations;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final JWTSecurity jwtSecurity;
    private final NotificationService notificationService;

    @Autowired
    public ExpenseController(ExpenseService expenseService,
                             JWTSecurity jwtSecurity,
                             NotificationService notificationService) {
        this.expenseService = expenseService;
        this.jwtSecurity = jwtSecurity;
        this.notificationService = notificationService;
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ResponseWrapper<ExpenseDTO>> getExpenseById(@PathVariable Long expenseId) {
        Result<ExpenseDTO> expenseResult = expenseService.getExpenseById(expenseId);
        if (!expenseResult.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseWrapper.notFound(expenseResult.getErrorMessage()));
        }

        return ResponseEntity.ok(ResponseWrapper.ok(expenseResult.getData(), "Expense Data Successfully Fetched"));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<ResponseWrapper<Page<ExpenseDTO>>> getExpenseByUserId(@PathVariable Long userId,
                                                                                @RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ExpenseDTO> expenseDTOPage = expenseService.getExpenseByUserId(userId, pageable);

        return ResponseEntity.ok(ResponseWrapper.ok(expenseDTOPage, "Expense Data Successfully Fetched"));
    }

    @GetMapping("/by-status")
    public ResponseEntity<ResponseWrapper<Page<ExpenseDTO>>> getExpensesByStatus(@RequestParam String status,
                                                                                @RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "10") int size,
                                                                                @RequestParam(defaultValue = "true") Boolean isSortedASC) {
        Sort.Direction direction = !isSortedASC ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "createdAt");

        // If params status received is not valid return PENDING as default
        ExpenseStatus expenseStatus = ExpenseStatus.findStatus(status).orElse(ExpenseStatus.PENDING);

        Pageable sortedPage = PageRequest.of(page, size, sort);
        Page<ExpenseDTO> expenseDTOPage = expenseService.getAllExpenseByStatus(expenseStatus, sortedPage);

        return ResponseEntity.ok(ResponseWrapper.ok(expenseDTOPage, "Expense Data Successfully Fetched. Sorted By: " + expenseStatus.toString() + " (" + direction +")"));
    }

    // Summary
    @GetMapping("/summary")
    public ResponseWrapper<ExpenseSummary> getExpenseSummaryByDateRange(@RequestParam(required = false) LocalDateTime startDate,
                                                                        @RequestParam(required = false) LocalDateTime endDate) {

        // If both startDate or endDate are null, get the current month summary
        if (startDate == null || endDate == null) {
            LocalDate currentDate = LocalDate.now();
            LocalDateTime startMonth = LocalDateTime.of(currentDate.getYear(), currentDate.getMonth(), 1, 0, 0);
            LocalDateTime endMonth = LocalDateTime.of(currentDate.getYear(), currentDate.getMonth(), currentDate.lengthOfMonth(), 23, 59, 59);

            // Fetch the monthly summary
            ExpenseSummary monthlySummary = expenseService.getExpenseSummaryByDateRange(startMonth, endMonth);
            return ResponseWrapper.ok(monthlySummary, "Monthly Expense Summary Successfully Fetched With Date Range: " + monthlySummary.getSummaryDateRange());
        }

        ExpenseSummary expenseSummary = expenseService.getExpenseSummaryByDateRange(startDate, endDate);

        return ResponseWrapper.ok(expenseSummary, "Expense Summary Successfully Fetched With Date Range: " + expenseSummary.getSummaryDateRange());
    }

    // Manger Role Auth
    @PutMapping("{expenseId}/approve")
    public ResponseEntity<ResponseWrapper<Page<ExpenseDTO>>> approveExpense(HttpServletRequest request,
                                                                            @PathVariable Long expenseId) {
        Result<Long> userIdResult = jwtSecurity.getUserIdFromToken(request);
        if (!userIdResult.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseWrapper.unauthorized(userIdResult.getErrorMessage()));
        }

        Result<ExpenseDTO> updatedResult = expenseService.approveExpense(expenseId, userIdResult.getData());
        if (!updatedResult.isSuccess()) {
            return ResponseEntity.status(updatedResult.getStatus()).body(ResponseWrapper.error(updatedResult.getErrorMessage(), updatedResult.getStatus().value()));
        }

        // Run in another thread and create and send the notification
        notificationService.sendNotificationFromExpense(updatedResult.getData());

        return ResponseEntity.ok(ResponseWrapper.ok(null, "Expense With Id " + expenseId + " Successfully Approve"));
    }

    @PutMapping("/{expenseId}/reject")
    public ResponseEntity<ResponseWrapper<Page<ExpenseDTO>>> rejectExpenseStatus(HttpServletRequest request,
                                                                                 @Valid ExpenseRejectDTO expenseRejectDTO,
                                                                                 BindingResult bindingResult) {
        Result<Long> userIdResult = jwtSecurity.getUserIdFromToken(request);
        if (!userIdResult.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseWrapper.unauthorized(userIdResult.getErrorMessage()));
        }

        Result<Void> validationResult = Validations.validateDTO(bindingResult);
        if (!validationResult.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseWrapper.badRequest(validationResult.getErrorMessage()));
        }

        Result<ExpenseDTO> updatedResult = expenseService.rejectExpense(expenseRejectDTO);
        if (!updatedResult.isSuccess()) {
            return ResponseEntity.status(updatedResult.getStatus()).body(ResponseWrapper.error(updatedResult.getErrorMessage(), updatedResult.getStatus().value()));
        }

        // Run in another thread and create and send the notification
        notificationService.sendNotificationFromExpense(updatedResult.getData());

        return ResponseEntity.ok(ResponseWrapper.ok(null, "Expense With Id " + expenseRejectDTO.getExpenseId() + " Successfully Reject"));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ResponseWrapper<ExpenseDTO>> softDeleteExpenseById(@PathVariable Long expenseId) {
        Result<Void> deleteResult = expenseService.softDeleteExpenseById(expenseId);
        if (!deleteResult.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseWrapper.notFound(deleteResult.getErrorMessage()));
        }

        return ResponseEntity.ok(ResponseWrapper.ok(null, "Expense Data Successfully Deleted"));
    }
}
