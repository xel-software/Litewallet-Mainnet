package nxt;

import nxt.db.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrencyBuyOffer extends CurrencyOffer {

    private static final DbKey.LongKeyFactory<CurrencyOffer> buyOfferDbKeyFactory = new DbKey.LongKeyFactory<CurrencyOffer>("id") {

        @Override
        public DbKey newKey(CurrencyOffer offer) {
            return offer.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencyOffer> buyOfferTable = new VersionedEntityDbTable<CurrencyOffer>("buy_offer", buyOfferDbKeyFactory) {

        @Override
        protected CurrencyBuyOffer load(Connection con, ResultSet rs) throws SQLException {
            return new CurrencyBuyOffer(rs);
        }

        @Override
        protected void save(Connection con, CurrencyOffer buy) throws SQLException {
            buy.save(con, table);
        }

    };

    public static int getCount() {
        return buyOfferTable.getCount();
    }

    public static CurrencyOffer getBuyOffer(long offerId) {
        return buyOfferTable.get(buyOfferDbKeyFactory.newKey(offerId));
    }

    public static DbIterator<CurrencyOffer> getAll(int from, int to) {
        return buyOfferTable.getAll(from, to);
    }

    //TODO: add index on rate DESC, height ASC, id ASC to buy_offer table?
    public static DbIterator<CurrencyOffer> getCurrencyOffers(long currencyId) {
        return buyOfferTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), 0, -1, " ORDER BY rate DESC, height ASC, id ASC ");
    }

    public static CurrencyOffer getCurrencyOffer(final long currencyId, final long accountId) {
        DbClause dbClause = new DbClause(" currency_id = ? AND account_id = ? ") {
            @Override
            protected int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, currencyId);
                pstmt.setLong(index++, accountId);
                return index;
            }
        };
        return buyOfferTable.getBy(dbClause);
    }

    static DbIterator<CurrencyOffer> getOffers(DbClause dbClause, int from, int to) {
        return buyOfferTable.getManyBy(dbClause, from, to);
    }

    static void addOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        buyOfferTable.insert(new CurrencyBuyOffer(transaction, attachment));
    }

    static void remove(CurrencyOffer buyOffer) {
        buyOfferTable.delete(buyOffer);
    }

    static void init() {}


    private CurrencyBuyOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getBuyRateNQT(),
                attachment.getTotalBuyLimit(), attachment.getInitialBuySupply(), attachment.getExpirationHeight(), transaction.getHeight());
        this.dbKey = buyOfferDbKeyFactory.newKey(id);
    }

    private CurrencyBuyOffer(ResultSet rs) throws SQLException {
        super(rs);
        this.dbKey = buyOfferDbKeyFactory.newKey(super.id);
    }

    protected void save(Connection con, String table) throws SQLException {
        super.save(con, table);
    }

    @Override
    public CurrencyOffer getCounterOffer() {
        return CurrencySellOffer.getSellOffer(id);
    }

    void increaseSupply(long delta) {
        super.increaseSupply(delta);
        buyOfferTable.insert(this);
    }

    void decreaseLimitAndSupply(long delta) {
        super.decreaseLimitAndSupply(delta);
        buyOfferTable.insert(this);
    }

}
