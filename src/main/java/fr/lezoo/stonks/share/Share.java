package fr.lezoo.stonks.share;

import fr.lezoo.stonks.Stonks;
import fr.lezoo.stonks.stock.Stock;
import fr.lezoo.stonks.util.Utils;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * One share can be bought by a player. This could be
 * saved in a hashMap if there weren't the leverage
 * parameter and the date at which it was bought
 */
public class Share {
    private final UUID uuid, owner;
    private final ShareType type;
    private final Stock stock;
    private final long timeStamp;
    private final double initialPrice;
    // if the share is closed it gets the final price and display it

    /**
     * The reason why the share was closed. Set to null
     * when it's still open
     */
    @Nullable
    private CloseReason closeReason;

    /**
     * Share price when it closed
     */
    private double sellPrice;

    /*
     * These fields can be modified by other plugins freely. maxPrice
     * and minPrice corresponds to the prices where it sells automatically
     */
    private final OrderInfo orderInfo = new OrderInfo();

    /**
     * Amount of money that can be claimed by the share owner
     * due to dividends.
     */
    private double wallet;

    /**
     * Used when shares are being created, bought or shorted.
     *
     * @param type     Type of share
     * @param stock    Stock
     * @param leverage Multiplicative factor for the money made out of,
     *                 or lost by a share purchase
     * @param shares   Amount of shares purchased
     */
    public Share(ShareType type, UUID owner, Stock stock, int leverage, double shares, double maxPrice, double minPrice) {
        this(UUID.randomUUID(), owner, type, stock, stock.getHandler().getShareInitialPrice(type == ShareType.NORMAL ? shares : -shares), leverage, shares, maxPrice, minPrice, System.currentTimeMillis());
    }

    /**
     * Public construtor
     *
     * @param uuid         Share unique identifier
     * @param owner        Share owner (player) UUID
     * @param type         Type of share
     * @param stock        Stock the share is from
     * @param initialPrice Stock price when stock was created
     * @param leverage     Multiplicative factor for the money made out of,
     *                     or lost by a share purchase
     * @param shares       Amount of shares purchased
     * @param timeStamp    Time of share creation (millis)
     */
    public Share(UUID uuid, UUID owner, ShareType type, Stock stock, double initialPrice, int leverage, double shares, double maxPrice, double minPrice, long timeStamp) {
        this.uuid = uuid;
        this.owner = owner;
        this.type = type;
        this.stock = stock;
        this.initialPrice = initialPrice;
        orderInfo.setLeverage(leverage);
        orderInfo.setAmount(shares);
        orderInfo.setMaxPrice(maxPrice);
        orderInfo.setMinPrice(minPrice);
        this.timeStamp = timeStamp;
    }

    /**
     * Loads a share from a config file
     */
    public Share(ConfigurationSection config) {
        this.uuid = UUID.fromString(config.getName());
        this.owner = UUID.fromString(config.getString("owner"));
        this.type = ShareType.valueOf(config.getString("type"));
        this.stock = Stonks.plugin.stockManager.get(config.getString("stock"));
        orderInfo.setAmount(config.getDouble("shares"));
        orderInfo.setLeverage(config.getInt("leverage"));
        this.timeStamp = config.getLong("timestamp");
        this.initialPrice = config.getDouble("initial");
        orderInfo.setMinPrice(config.getDouble("min-price"));
        orderInfo.setMaxPrice(config.getDouble("max-price"));
        this.wallet = config.getDouble("wallet");
        this.closeReason = config.contains("close-reason") ? CloseReason.valueOf(config.getString("close-reason")) : null;
        this.sellPrice = isOpen() ? 0 : config.getDouble("sell-price");
    }

    /**
     * Reads a share from a container in which share data was saved.
     * See {@link fr.lezoo.stonks.item.SharePaper} to see the data format
     *
     * @param container Item container to read
     */
    public Share(UUID owner, PersistentDataContainer container) {

        // Info generated randomly
        this.owner = owner;
        this.uuid = UUID.randomUUID();

        // Mandatory info
        this.type = ShareType.valueOf(container.get(Utils.namespacedKey("ShareType"), PersistentDataType.STRING));
        this.stock = Objects.requireNonNull(Stonks.plugin.stockManager.get(container.get(Utils.namespacedKey("StockId"), PersistentDataType.STRING)), "Could not find stock");
        this.timeStamp = container.get(Utils.namespacedKey("ShareTimeStamp"), PersistentDataType.LONG);
        this.initialPrice = container.get(Utils.namespacedKey("ShareInitialPrice"), PersistentDataType.DOUBLE);

        // Non final info
        orderInfo.setAmount(container.get(Utils.namespacedKey("ShareAmount"), PersistentDataType.DOUBLE));
        orderInfo.setLeverage(container.get(Utils.namespacedKey("ShareLeverage"), PersistentDataType.INTEGER));
        this.wallet = container.get(Utils.namespacedKey("ShareWallet"), PersistentDataType.DOUBLE);
        this.closeReason = container.has(Utils.namespacedKey("CloseReason"), PersistentDataType.STRING) ? CloseReason.valueOf(container.get(Utils.namespacedKey("CloseReason"), PersistentDataType.STRING)) : null;
        this.sellPrice = isOpen() ? 0 : container.get(Utils.namespacedKey("SellPrice"), PersistentDataType.DOUBLE);
    }

    public void saveInConfig(ConfigurationSection config) {
        config.set(uuid + ".type", type.name());
        config.set(uuid + ".owner", owner.toString());
        config.set(uuid + ".stock", stock.getId());
        config.set(uuid + ".shares", orderInfo.getAmount());
        config.set(uuid + ".leverage", orderInfo.getLeverage());
        config.set(uuid + ".timestamp", timeStamp);
        config.set(uuid + ".initial", initialPrice);
        config.set(uuid + ".max-price", orderInfo.getMaxPrice());
        config.set(uuid + ".min-price", orderInfo.getMinPrice());
        config.set(uuid + ".wallet", wallet);
        if (!isOpen()) {
            config.set(uuid + ".close-reason", closeReason.name());
            config.set(uuid + ".sell-price", sellPrice);
        }
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public UUID getOwner() {
        return owner;
    }

    public ShareType getType() {
        return type;
    }

    public Stock getStock() {
        return stock;
    }

    /**
     * @return Time (in millis) at which the share was created
     */
    public long getCreationTime() {
        return timeStamp;
    }

    public double getInitialPrice() {
        return initialPrice;
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

    /**
     * @return Money given by dividends waiting to be claimed by the player
     */
    public double getWallet() {
        return wallet;
    }

    public double getMaxPrice() {
        return orderInfo.getMaxPrice();
    }

    public double getMinPrice() {
        return orderInfo.getMinPrice();
    }

    public String getStringMinPrice() {
        return orderInfo.getMinPrice() == 0 ? "none" : Utils.fourDigits.format(orderInfo.getMinPrice());
    }

    public String getStringMaxPrice() {
        return orderInfo.getMaxPrice() == Double.POSITIVE_INFINITY ? "none" : Utils.fourDigits.format(orderInfo.getMaxPrice());
    }

    @NotNull
    public CloseReason getCloseReason() {
        return Objects.requireNonNull(closeReason, "Share is open");
    }

    public double getShares() {
        return orderInfo.getAmount();
    }

    public boolean isOpen() {
        return closeReason == null;
    }

    /**
     * Closing a share differs from saving it. Closing a share
     * freezes its sell price. Claiming it gives the money
     * back to the player.
     * <p>
     * Closed shares with negative benefits should be automatically
     * claimed by the player who has no interest in manually
     * claiming them (resulting in a money loss)
     *
     * @param closeReason Why the share was closed
     */
    public void close(@NotNull CloseReason closeReason) {
        Validate.isTrue(isOpen(), "Share is already closed");
        this.closeReason = Objects.requireNonNull(closeReason, "Reason cannot be null");
        this.sellPrice = stock.getPrice();
        //You make it as if you bought the counter order
        stock.getHandler().whenBought(type.equals(ShareType.NORMAL) ? -getShares() : getShares());
    }

    public void setWallet(double wallet) {
        this.wallet = wallet;
    }

    /**
     * Called when dividends are applied to the share.
     *
     * @param gain Money that can be claimed by the player.
     */
    public void addToWallet(double gain) {
        Validate.isTrue(gain >= 0, "Gain must be positive");

        wallet += gain;
    }

    /**
     * Does not take dividends wallet into account
     * <p>
     * The share cannot be valued negatively
     *
     * @param taxRate Rate of tax on benefits
     * @return Money earned by the player if he were to close
     *         this share right now.
     */
    public double getCloseEarning(double taxRate) {
        return Math.max(calculateGain(taxRate) + initialPrice * orderInfo.getAmount(), 0);
    }

    /**
     * Does not take dividends wallet into account
     *
     * @return Money gained by the player (may be negative)
     *         if they were to claim the share right now
     */
    public double calculateGain(double taxRate) {

        // Find applicable share price
        double sharePrice = isOpen() ? stock.getPrice() : sellPrice;

        // <Difference in price between when it was bought and now> * <leverage> * <number of shares>
        double gain = (sharePrice - initialPrice) * (type == ShareType.SHORT ? -1 : 1) * orderInfo.getLeverage() * orderInfo.getAmount();

        // Tax on benefits only
        if (gain > 0)
            gain *= 1 - taxRate;

        return gain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Share share = (Share) o;
        return Objects.equals(uuid, share.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
