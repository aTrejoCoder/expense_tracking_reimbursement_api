package alexisTrejo.expenses.tracking.api.Mappers;

import alexisTrejo.expenses.tracking.api.DTOs.Expenses.ExpenseDTO;
import alexisTrejo.expenses.tracking.api.DTOs.Expenses.ExpenseInsertDTO;
import alexisTrejo.expenses.tracking.api.Models.Expense;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2024-10-20T22:44:09-0600",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.9.jar, environment: Java 17.0.11 (Amazon.com Inc.)"
)
@Component
public class ExpenseMapperImpl implements ExpenseMapper {

    @Override
    public Expense insertDtoToEntity(ExpenseInsertDTO expenseInsertDTO) {
        if ( expenseInsertDTO == null ) {
            return null;
        }

        Expense expense = new Expense();

        return expense;
    }

    @Override
    public ExpenseDTO entityToDTO(Expense expense) {
        if ( expense == null ) {
            return null;
        }

        ExpenseDTO expenseDTO = new ExpenseDTO();

        return expenseDTO;
    }
}
