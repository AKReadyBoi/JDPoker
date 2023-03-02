public class PokerPlayer {
    private int balance;
    private int firstparam;
    private int firstparam1;
    private String secondparam;
    private String secondparam1;
    private final String discordTag;
    public boolean isFold=false;
    public boolean isAllIn=false;
    private final long id;
    public PokerPlayer(int balance, int firstparam, String secondparam, int firstparam1, String secondparam1, String discordTag, long id) {
        this.balance = balance;
        this.firstparam = firstparam;
        this.secondparam = secondparam;
        this.firstparam1 = firstparam1;
        this.secondparam1 = secondparam1;
        this.discordTag = discordTag;
        this.id = id;
    }
//    public PokerPlayer getPlayerByDiscordTag(String discordTag) {
//
//    }
    public String getDiscordTag() {
        return discordTag;
    }
    public long getId() {
        return id;
    }
    public int getBalance() {
        return balance;
    }
    public int getFirstparam() {
        return firstparam;
    }
    public int getFirstparam1() {
        return firstparam1;
    }
    public String getSecondparam() {
        return secondparam;
    }
    public String getSecondparam1() {
        return secondparam1;
    }
    public void setBalance(int balance) {
        this.balance=balance;
    }
    public void setFirstparam(int firstparam) {
        this.firstparam = firstparam;
    }
    public void setFirstparam1(int firstparam1) {
        this.firstparam1 = firstparam1;
    }
    public void setSecondparam(String secondparam) {
        this.secondparam = secondparam;
    }
    public void setSecondparam1(String secondparam1) {
        this.secondparam1 = secondparam1;
    }
}
