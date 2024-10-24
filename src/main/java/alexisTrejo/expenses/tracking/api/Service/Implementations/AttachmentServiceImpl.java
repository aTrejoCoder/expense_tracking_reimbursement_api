package alexisTrejo.expenses.tracking.api.Service.Implementations;

import alexisTrejo.expenses.tracking.api.DTOs.Attachements.AttachmentDTO;
import alexisTrejo.expenses.tracking.api.Mappers.AttachmentMapper;
import alexisTrejo.expenses.tracking.api.Models.Expense;
import alexisTrejo.expenses.tracking.api.Models.ExpenseAttachment;
import alexisTrejo.expenses.tracking.api.Repository.ExpenseRepository;
import alexisTrejo.expenses.tracking.api.Service.Interfaces.AttachmentService;
import alexisTrejo.expenses.tracking.api.Utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final ExpenseRepository expenseRepository;
    private final AttachmentMapper attachmentMapper;

    @Autowired
    public AttachmentServiceImpl(ExpenseRepository expenseRepository,
                                 AttachmentMapper attachmentMapper) {
        this.expenseRepository = expenseRepository;
        this.attachmentMapper = attachmentMapper;
    }

    @Override
    public Result<List<AttachmentDTO>> getAttachmentsByExpenseId(Long expenseId) {
            Optional<Expense> optionalExpense = expenseRepository.findById(expenseId);
             return optionalExpense.map(expense -> {
                        List<AttachmentDTO> attachmentDTOS = expense.getExpenseAttachments()
                                .stream()
                                .map(attachmentMapper::entityToDTO)
                                .toList();

                        return Result.success(attachmentDTOS);
                    }).orElseGet(() -> Result.error("Expense with ID " + expenseId + " not found"));
    }

    @Override
    public void createAttachment(Long expenseId, String fileURL) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense Not Found"));

        ExpenseAttachment expenseAttachment = new ExpenseAttachment(expense, fileURL);

        expense.addAttachment(expenseAttachment);

        expenseRepository.saveAndFlush(expense);
    }
}
