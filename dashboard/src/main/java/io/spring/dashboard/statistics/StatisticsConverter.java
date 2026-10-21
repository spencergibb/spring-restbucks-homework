package io.spring.dashboard.statistics;

import java.util.Locale;
import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import org.springframework.stereotype.Component;

/**
 * Converter for formatting monetary amounts in dashboard statistics.
 * Formats amounts using US locale with proper thousand separators and decimal formatting.
 */
@Component
public class StatisticsConverter {

    private final MonetaryAmountFormat formatter = MonetaryFormats.getAmountFormat(Locale.US);

    /**
     * Converts a MonetaryAmount to a formatted string for display in statistics.
     * 
     * @param amount the monetary amount to format
     * @return formatted string representation of the amount
     */
    public String formatAmount(MonetaryAmount amount) {
        if (amount == null) {
            return null;
        }
        
        String formattedAmount = formatter.format(amount);
        
        // Ensure there's a space between currency code and amount
        if (formattedAmount.startsWith("EUR") && !formattedAmount.startsWith("EUR ")) {
            formattedAmount = formattedAmount.replace("EUR", "EUR ");
        }
        
        return formattedAmount;
    }
}