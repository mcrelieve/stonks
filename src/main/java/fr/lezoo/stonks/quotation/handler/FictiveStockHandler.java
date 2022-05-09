package fr.lezoo.stonks.quotation.handler;

import fr.lezoo.stonks.Stonks;
import fr.lezoo.stonks.quotation.Quotation;
import fr.lezoo.stonks.quotation.QuotationInfo;
import fr.lezoo.stonks.quotation.TimeScale;
import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static fr.lezoo.stonks.quotation.Quotation.BOARD_DATA_NUMBER;

public class FictiveStockHandler implements StockHandler {
    private final Quotation quotation;

    private double priceMultiplier;
    private final double initialSupply;
    //Initial supply + supply by people
    private double totalSupply;

    private static final Random RANDOM = new Random();

    public FictiveStockHandler(Quotation quotation, double initialSupply) {
        this.quotation = quotation;
        this.initialSupply = initialSupply;
        //We setup the price multiplier
        this.priceMultiplier = quotation.getPrice() / initialSupply;
        this.totalSupply = initialSupply;
        //p=calculatePrice()*priceMultiplier();

    }

    public FictiveStockHandler(Quotation quotation, ConfigurationSection config) {
        this.quotation = quotation;

        initialSupply = config.getDouble("initial-supply");
        totalSupply = config.contains("total-supply") ? config.getDouble("total-supply") : initialSupply;
        priceMultiplier = config.contains("price-multiplier") ? config.getDouble("price-multiplier"):quotation.getPrice() / initialSupply  ;
    }


    @Override
    public void refresh() {
        double price = calculatePrice(totalSupply);
        //We setup the price in the quotation
        for (TimeScale time : TimeScale.values()) {

            // We get the list corresponding to the time
            List<QuotationInfo> workingData = new ArrayList<>();
            workingData.addAll(quotation.getData(time));

            // This fixes an issue with empty working data
            long lastTimeStamp = workingData.isEmpty() ? 0 : workingData.get(workingData.size() - 1).getTimeStamp();

            // If the the latest data of workingData is too old we add another one
            if (System.currentTimeMillis() - lastTimeStamp > time.getTime() / Quotation.BOARD_DATA_NUMBER) {

                workingData.add(new QuotationInfo(System.currentTimeMillis(), price));
                // If the list contains too much data we remove the older ones
                if (workingData.size() > Quotation.BOARD_DATA_NUMBER)
                    workingData.remove(0);
                // We save the changes we made in the attribute
                quotation.setData(time, workingData);
            }
        }
    }



    @Override
    public void whenBought(double stocksBought) {
        totalSupply += (int) stocksBought;
    }

    @Override
    public void saveInFile(ConfigurationSection config) {
        config.set("initial-supply", initialSupply);
        config.set("total-supply", totalSupply);
        config.set("price-multiplier", priceMultiplier);
    }

    /**
     * Calculates the price the quotation should have at any moment
     */
    //Calculate the price the course will have if you buy shares
    public double calculatePrice(double totalSup) {
        return priceMultiplier * (totalSup < initialSupply / 10 ? calculateExponential(totalSup) : totalSup);
    }


    public double calculateExponential(double totalSup) {
        return initialSupply / 10 * (Math.exp(totalSup - (initialSupply / 10)));
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    public double getInitialSupply() {
        return initialSupply;
    }

    public double getTotalSupply() {
        return totalSupply;
    }


    @Override
    //Formula for the change in price over time
    public void refreshPrice() {
        //We just refresh the priceMultiplier
        priceMultiplier *= 1 + (RANDOM.nextDouble() - 0.5) * Stonks.plugin.configManager.volatility * Math.sqrt(quotation.getRefreshPeriod()) /
                10*(Math.sqrt(TimeScale.HOUR.getTime()));
    }

}
