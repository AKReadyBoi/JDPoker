import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/* подправить вывод:
        2) сделать малые и большие блайнды
        5) одноразовое использование
 */
public class Poker extends ListenerAdapter {
    static JDA jda;
    static ArrayList<String> cards = new ArrayList<>();
    static ArrayList<String> suits = new ArrayList<>();
    static ArrayList<Integer> numbers = new ArrayList<>();
    static ArrayList<Integer> deckNumbers = new ArrayList<>();
    static ArrayList<String> deckSuits = new ArrayList<>();
    static HashMap<PokerPlayer, Long> playerValues = new HashMap<>();
    static boolean extraOne = true;
    static int pot = 0;
    static ArrayList<String> names = new ArrayList<>();
    static ArrayList<Long> ids = new ArrayList<>();
    static TextChannel channel;
    static VoiceChannel connectedChannel;
    static boolean isPlaying = false;
    static int playersAmount;
    static Message lastMessage;
    private static final Lock lock = new ReentrantLock();
    private static final Condition messageReceived = lock.newCondition();
    static StringBuffer cardPool = new StringBuffer();

    public static void main(String[] args) throws InterruptedException {
        jda = JDABuilder.createDefault("token")
                .enableIntents(GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGE_TYPING,
                        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                        GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.playing("дифиченто"))
                .addEventListeners(new Poker())
                .build();
        jda.awaitReady();
        suits.add(0, "♠");
        suits.add(1, "♣");
        suits.add(2, "♥");
        suits.add(3, "♦");
        for (int i = 2; i <= 14; i++) {
            numbers.add(i);
        }
        lock.lock();
        try {
            messageReceived.await();
        } finally {
            lock.unlock();
        }
        new Poker().start();
    }
    public void start() throws InterruptedException {
        if (isPlaying) {
            ArrayList<PokerPlayer> playersIn = new ArrayList<>();
            ArrayList<PokerPlayer> playersActive = new ArrayList<>(playersIn);
            for (int i = 0; i < playersAmount; i++) {
                PokerPlayer pokerPlayer = new PokerPlayer(10000, 1, "a", 1, "a", names.get(i), ids.get(i));
                playersIn.add(pokerPlayer);
            }
            ArrayList<Integer> betAmounts = new ArrayList<>(playersAmount);
            for (int i = 0; i < playersIn.size(); i++) {
                betAmounts.add(i, 0);
            }
            playersActive.addAll(playersIn);
            while (playersActive.size() != 1) {
                boolean onePlayerLeft = false;
                int lastRoundBet = 0;
                for (PokerPlayer player : playersIn) {
                    new Poker().shuffle(player);
                    User user = Poker.jda.retrieveUserById(player.getId()).complete();
                    user.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessage(String.valueOf(player.getFirstparam()).replaceAll("\\b10\\b", "T").replaceAll("\\b11\\b", "J").replaceAll("\\b12\\b", "Q").replaceAll("\\b13\\b", "K").replaceAll("(14|1)", "A") + player.getSecondparam() + " " + String.valueOf(player.getFirstparam1()).replaceAll("\\b10\\b", "T").replaceAll("\\b11\\b", "J").replaceAll("\\b12\\b", "Q").replaceAll("\\b13\\b", "K").replaceAll("(14|1)", "A") + player.getSecondparam1())).queue();
                }
                do {
                    for (int i = 0; i < playersIn.size(); i++) {
                        if (!(playersIn.get(i).isFold || playersIn.get(i).isAllIn)) {
                            lock.lock();
                            try {
                                messageReceived.await();
                            } finally {
                                lock.unlock();
                            }
                            if ((lastMessage.getAuthor().getName() + "#" + lastMessage.getAuthor().getDiscriminator()).equals(playersIn.get(i).getDiscordTag())) {
                                String betAmount1 = lastMessage.getContentRaw();
                                if (betAmount1.toLowerCase(Locale.ROOT).equals("fold")) {
                                    channel.sendMessage("Вы вышли из игры").queue();
                                    playersIn.get(i).isFold = true;
                                } else {
                                    try {
                                        if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) > 0 && Integer.parseInt(betAmount1) >= 0) {
                                            if (i == 0 && lastRoundBet == betAmounts.get(0)) {
                                                playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                pot = pot + Integer.parseInt(betAmount1);
                                                int j = betAmounts.get(i);
                                                betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                            } else {
                                                if (i == 0) {
                                                    if (Integer.parseInt(betAmount1) + betAmounts.get(i) >= betAmounts.get(i + 1)) {
                                                        playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                        pot = pot + Integer.parseInt(betAmount1);
                                                        int j = betAmounts.get(i);
                                                        betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                    } else {
                                                        channel.sendMessage("Нужно больше золота").queue();
                                                        i--;
                                                    }
                                                } else {
                                                    if (Integer.parseInt(betAmount1) + betAmounts.get(i) >= betAmounts.get(i - 1)) {
                                                        playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                        pot = pot + Integer.parseInt(betAmount1);
                                                        int j = betAmounts.get(i);
                                                        betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                    } else {
                                                        channel.sendMessage("Нужно больше золота").queue();
                                                        i--;
                                                    }
                                                }
                                            }
                                        } else {
                                            if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) == 0 && Integer.parseInt(betAmount1) >= 0) {
                                                playersIn.get(i).isAllIn = true;
                                                channel.sendMessage("You`re All-In!").queue();
                                                playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                pot = pot + Integer.parseInt(betAmount1);
                                                int alt = betAmounts.get(i);
                                                betAmounts.set(i, Integer.parseInt(betAmount1) + alt);
                                            } else {
                                                channel.sendMessage("Нет средств").queue();
                                                i--;
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        i--;
                                    }

                                    for (int j = 0; j < playersIn.size(); j++) {
                                        if (playersIn.get(j).isFold||playersIn.get(j).isAllIn) {
                                            betAmounts.remove(j);
                                            playersIn.remove(playersIn.get(j));
                                            i--;
                                        }
                                    }

                                    for (PokerPlayer player : playersIn) {
                                        channel.sendMessage(player.getDiscordTag() + " :" + betAmounts.get(playersIn.indexOf(player))).queue();
                                    }

                                    if (playersIn.size() == 1) {
                                        onePlayerLeft = true;
                                    }
                                    if (new HashSet<>(betAmounts).size() == 1 && !(new HashSet<>(betAmounts).size() == 1 && new HashSet<>(betAmounts).contains(lastRoundBet))) {
                                        channel.sendMessage("Current pot: " + pot).queue();
                                        break;
                                    }
                                }
                            } else {
                                i--;
                            }
                        }
                    }
                }
                while (new HashSet<>(betAmounts).size() != 1);
                lastRoundBet = betAmounts.get(0);
                new Poker().shuffle();
                new Poker().shuffle();
                new Poker().shuffle();
                channel.sendMessage(cardPool).queue();
                if (!onePlayerLeft) {
                    do {
                        for (int i = 0; i < playersIn.size(); i++) {
                            if (!(playersIn.get(i).isFold || playersIn.get(i).isAllIn)) {
                                lock.lock();
                                try {
                                    messageReceived.await();
                                } finally {
                                    lock.unlock();
                                }
                                if ((lastMessage.getAuthor().getName() + "#" + lastMessage.getAuthor().getDiscriminator()).equals(playersIn.get(i).getDiscordTag())) {
                                    String betAmount1 = lastMessage.getContentRaw();
                                    if (betAmount1.toLowerCase(Locale.ROOT).equals("fold")) {
                                        channel.sendMessage("Вы вышли из игры").queue();
                                        playersIn.get(i).isFold = true;
                                    } else {
                                        try {
                                            if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) > 0 && Integer.parseInt(betAmount1) >= 0) {
                                                if (i == 0 && lastRoundBet == betAmounts.get(0)) {
                                                    playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                    pot = pot + Integer.parseInt(betAmount1);
                                                    int j = betAmounts.get(i);
                                                    betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                } else {
                                                    if (i == 0) {
                                                        if (Integer.parseInt(betAmount1) + betAmounts.get(i) >= betAmounts.get(i + 1)) {
                                                            playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                            pot = pot + Integer.parseInt(betAmount1);
                                                            int j = betAmounts.get(i);
                                                            betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                        } else {
                                                            channel.sendMessage("Нужно больше золота").queue();
                                                            i--;
                                                        }
                                                    } else {
                                                        if (Integer.parseInt(betAmount1) + betAmounts.get(i) >= betAmounts.get(i - 1)) {
                                                            playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                            pot = pot + Integer.parseInt(betAmount1);
                                                            int j = betAmounts.get(i);
                                                            betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                        } else {
                                                            channel.sendMessage("Нужно больше золота").queue();
                                                            i--;
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) == 0 && Integer.parseInt(betAmount1) >= 0) {
                                                    playersIn.get(i).isAllIn = true;
                                                    channel.sendMessage("You`re All-In!").queue();
                                                    playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                    pot = pot + Integer.parseInt(betAmount1);
                                                    int alt = betAmounts.get(i);
                                                    betAmounts.set(i, Integer.parseInt(betAmount1) + alt);
                                                } else {
                                                    channel.sendMessage("Нет средств").queue();
                                                    i--;
                                                }
                                            }
                                        } catch (NumberFormatException e) {
                                            i--;
                                        }

                                        for (int j = 0; j < playersIn.size(); j++) {
                                            if (playersIn.get(j).isFold||playersIn.get(j).isAllIn) {
                                                betAmounts.remove(j);
                                                playersIn.remove(playersIn.get(j));
                                                i--;
                                            }
                                        }

                                        for (PokerPlayer player : playersIn) {
                                            channel.sendMessage(player.getDiscordTag() + " :" + betAmounts.get(playersIn.indexOf(player))).queue();
                                        }

                                        if (playersIn.size() == 1) {
                                            onePlayerLeft = true;
                                        }
                                        if (new HashSet<>(betAmounts).size() == 1 && !(new HashSet<>(betAmounts).size() == 1 && new HashSet<>(betAmounts).contains(lastRoundBet))) {
                                            channel.sendMessage("Current pot: " + pot).queue();
                                            break;
                                        }
                                    }
                                } else {
                                    i--;
                                }
                            }
                        }
                    }
                    while (new HashSet<>(betAmounts).size() != 1);
                }
                lastRoundBet = betAmounts.get(0);
                new Poker().shuffle();
                channel.sendMessage(cardPool).queue();
                if (!onePlayerLeft) {
                    do {
                        for (int i = 0; i < playersIn.size(); i++) {
                            if (!(playersIn.get(i).isFold || playersIn.get(i).isAllIn)) {
                                lock.lock();
                                try {
                                    messageReceived.await();
                                } finally {
                                    lock.unlock();
                                }
                                if ((lastMessage.getAuthor().getName() + "#" + lastMessage.getAuthor().getDiscriminator()).equals(playersIn.get(i).getDiscordTag())) {
                                    String betAmount1 = lastMessage.getContentRaw();
                                    if (betAmount1.toLowerCase(Locale.ROOT).equals("fold")) {
                                        channel.sendMessage("Вы вышли из игры").queue();
                                        playersIn.get(i).isFold = true;
                                    } else {
                                        try {
                                            if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) > 0 && Integer.parseInt(betAmount1) >= 0) {
                                                if (i == 0 && lastRoundBet == betAmounts.get(0)) {
                                                    playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                    pot = pot + Integer.parseInt(betAmount1);
                                                    int j = betAmounts.get(i);
                                                    betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                } else {
                                                    if (i == 0) {
                                                        if (Integer.parseInt(betAmount1) + betAmounts.get(i) >= betAmounts.get(i + 1)) {
                                                            playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                            pot = pot + Integer.parseInt(betAmount1);
                                                            int j = betAmounts.get(i);
                                                            betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                        } else {
                                                            channel.sendMessage("Нужно больше золота").queue();
                                                            i--;
                                                        }
                                                    } else {
                                                        if (Integer.parseInt(betAmount1) + betAmounts.get(i) >= betAmounts.get(i - 1)) {
                                                            playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                            pot = pot + Integer.parseInt(betAmount1);
                                                            int j = betAmounts.get(i);
                                                            betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                        } else {
                                                            channel.sendMessage("Нужно больше золота").queue();
                                                            i--;
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) == 0 && Integer.parseInt(betAmount1) >= 0) {
                                                    playersIn.get(i).isAllIn = true;
                                                    channel.sendMessage("You`re All-In!").queue();
                                                    playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                    pot = pot + Integer.parseInt(betAmount1);
                                                    int alt = betAmounts.get(i);
                                                    betAmounts.set(i, Integer.parseInt(betAmount1) + alt);
                                                } else {
                                                    channel.sendMessage("Нет средств").queue();
                                                    i--;
                                                }
                                            }
                                        } catch (NumberFormatException e) {
                                            i--;
                                        }

                                        for (int j = 0; j < playersIn.size(); j++) {
                                            if (playersIn.get(j).isFold||playersIn.get(j).isAllIn) {
                                                betAmounts.remove(j);
                                                playersIn.remove(playersIn.get(j));
                                                i--;
                                            }
                                        }

                                        for (PokerPlayer player : playersIn) {
                                            channel.sendMessage(player.getDiscordTag() + " :" + betAmounts.get(playersIn.indexOf(player))).queue();
                                        }

                                        if (playersIn.size() == 1) {
                                            onePlayerLeft = true;
                                        }
                                        if (new HashSet<>(betAmounts).size() == 1 && !(new HashSet<>(betAmounts).size() == 1 && new HashSet<>(betAmounts).contains(lastRoundBet))) {
                                            channel.sendMessage("Current pot: " + pot).queue();
                                            break;
                                        }
                                    }
                                } else {
                                    i--;
                                }
                            }
                        }
                    }
                    while (new HashSet<>(betAmounts).size() != 1);
                }
                lastRoundBet = betAmounts.get(0);
                new Poker().shuffle();
                channel.sendMessage(cardPool).queue();
                if (!onePlayerLeft) {
                    do {
                        for (int i = 0; i < playersIn.size(); i++) {
                            if (!(playersIn.get(i).isFold || playersIn.get(i).isAllIn)) {
                                lock.lock();
                                try {
                                    messageReceived.await();
                                } finally {
                                    lock.unlock();
                                }
                                if ((lastMessage.getAuthor().getName() + "#" + lastMessage.getAuthor().getDiscriminator()).equals(playersIn.get(i).getDiscordTag())) {
                                    String betAmount1 = lastMessage.getContentRaw();
                                    if (betAmount1.toLowerCase(Locale.ROOT).equals("fold")) {
                                        channel.sendMessage("Вы вышли из игры").queue();
                                        playersIn.get(i).isFold = true;
                                    } else {
                                        try {
                                            if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) > 0 && Integer.parseInt(betAmount1) >= 0) {
                                                if (i == 0 && lastRoundBet == betAmounts.get(0)) {
                                                    playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                    pot = pot + Integer.parseInt(betAmount1);
                                                    int j = betAmounts.get(i);
                                                    betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                } else {
                                                    if (i == 0) {
                                                        if (Integer.parseInt(betAmount1) + betAmounts.get(i) >= betAmounts.get(i + 1)) {
                                                            playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                            pot = pot + Integer.parseInt(betAmount1);
                                                            int j = betAmounts.get(i);
                                                            betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                        } else {
                                                            channel.sendMessage("Нужно больше золота").queue();
                                                            i--;
                                                        }
                                                    } else {
                                                        if (Integer.parseInt(betAmount1) + betAmounts.get(i) >= betAmounts.get(i - 1)) {
                                                            playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                            pot = pot + Integer.parseInt(betAmount1);
                                                            int j = betAmounts.get(i);
                                                            betAmounts.set(i, Integer.parseInt(betAmount1) + j);
                                                        } else {
                                                            channel.sendMessage("Нужно больше золота").queue();
                                                            i--;
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) == 0 && Integer.parseInt(betAmount1) >= 0) {
                                                    playersIn.get(i).isAllIn = true;
                                                    channel.sendMessage("You`re All-In!").queue();
                                                    playersIn.get(i).setBalance(playersIn.get(i).getBalance() - Integer.parseInt(betAmount1));
                                                    pot = pot + Integer.parseInt(betAmount1);
                                                    int alt = betAmounts.get(i);
                                                    betAmounts.set(i, Integer.parseInt(betAmount1) + alt);
                                                } else {
                                                    channel.sendMessage("Нет средств").queue();
                                                    i--;
                                                }
                                            }
                                        } catch (NumberFormatException e) {
                                            i--;
                                        }

                                        for (int j = 0; j < playersIn.size(); j++) {
                                            if (playersIn.get(j).isFold||playersIn.get(j).isAllIn) {
                                                betAmounts.remove(j);
                                                playersIn.remove(playersIn.get(j));
                                                i--;
                                            }
                                        }

                                        for (PokerPlayer player : playersIn) {
                                            channel.sendMessage(player.getDiscordTag() + " :" + betAmounts.get(playersIn.indexOf(player))).queue();
                                        }

                                        if (playersIn.size() == 1) {
                                            break;
                                        }
                                        if (new HashSet<>(betAmounts).size() == 1 && !(new HashSet<>(betAmounts).size() == 1 && new HashSet<>(betAmounts).contains(lastRoundBet))) {
                                            channel.sendMessage("Current pot: " + pot).queue();
                                            break;
                                        }
                                    }
                                } else {
                                    i--;
                                }
                            }
                        }
                    }
                    while (new HashSet<>(betAmounts).size() != 1);
                    for (PokerPlayer player : playersActive) {
                        if (player.isAllIn) {
                            playersIn.add(player);
                        }
                    }
                    for (PokerPlayer player : playersIn) {
                        deckNumbers.add(player.getFirstparam());
                        deckNumbers.add(player.getFirstparam1());
                        deckSuits.add(player.getSecondparam());
                        deckSuits.add(player.getSecondparam1());
                        playerValues.put(player, new Poker().maxValueHandEvaluation(deckNumbers, deckSuits));
                        deckNumbers.remove((Object) player.getFirstparam());
                        deckNumbers.remove((Object) player.getFirstparam1());
                        deckSuits.remove(player.getSecondparam());
                        deckSuits.remove(player.getSecondparam1());
                    }
                    LinkedHashMap<PokerPlayer, Long> reverseSortedMap = new LinkedHashMap<>();
                    playerValues.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
                    Map.Entry<PokerPlayer, Long> firstEntry = reverseSortedMap.entrySet().iterator().next();
                    ArrayList<PokerPlayer> repeatingKeys = new ArrayList<>();
                    for (Map.Entry<PokerPlayer, Long> entry : reverseSortedMap.entrySet()) {
                        if (entry.getValue().equals(firstEntry.getValue())) {
                            repeatingKeys.add(entry.getKey());
                        }
                    }
                    for (PokerPlayer repeatingKey : repeatingKeys) {
                        repeatingKey.setBalance(repeatingKey.getBalance() + pot/repeatingKeys.size());
                    }
                    reverseSortedMap.forEach((key, value) -> channel.sendMessage(key.getDiscordTag() + "  has cards:" + String.valueOf(key.getFirstparam()).replaceAll("\\b10\\b", "T").replaceAll("\\b11\\b", "J").replaceAll("\\b12\\b", "Q").replaceAll("\\b13\\b", "K").replaceAll("(14|1)", "A") + key.getSecondparam() + " " + String.valueOf(key.getFirstparam1()).replaceAll("\\b10\\b", "T").replaceAll("\\b11\\b", "J").replaceAll("\\b12\\b", "Q").replaceAll("\\b13\\b", "K").replaceAll("(14|1)", "A") + key.getSecondparam1()).queue());
                    for (PokerPlayer player : playersActive) {
                        channel.sendMessage(player.getDiscordTag() + " current Balance:" + player.getBalance()).queue();
                    }
                } else {
                    for (PokerPlayer player : playersActive) {
                        if (player.isAllIn) {
                            playersIn.add(player);
                        }
                    }
                    for (PokerPlayer players : playersIn) {
                        deckNumbers.add(players.getFirstparam());
                        deckNumbers.add(players.getFirstparam1());
                        deckSuits.add(players.getSecondparam());
                        deckSuits.add(players.getSecondparam1());
                        playerValues.put(players, new Poker().maxValueHandEvaluation(deckNumbers, deckSuits));
                        deckNumbers.remove((Object) players.getFirstparam());
                        deckNumbers.remove((Object) players.getFirstparam1());
                        deckSuits.remove(players.getSecondparam());
                        deckSuits.remove(players.getSecondparam1());
                    }
                    LinkedHashMap<PokerPlayer, Long> reverseSortedMap = new LinkedHashMap<>();
                    playerValues.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
                    Map.Entry<PokerPlayer, Long> firstEntry = reverseSortedMap.entrySet().iterator().next();
                    ArrayList<PokerPlayer> repeatingKeys = new ArrayList<>();
                    for (Map.Entry<PokerPlayer, Long> entry : reverseSortedMap.entrySet()) {
                        if (entry.getValue().equals(firstEntry.getValue())) {
                            repeatingKeys.add(entry.getKey());
                        }
                    }
                    for (PokerPlayer repeatingKey : repeatingKeys) {
                        repeatingKey.setBalance(repeatingKey.getBalance() + pot/repeatingKeys.size());
                    }
                    reverseSortedMap.forEach((key, value) -> channel.sendMessage(key.getDiscordTag() + "  has cards:" + String.valueOf(key.getFirstparam()).replaceAll("\\b10\\b", "T").replaceAll("\\b11\\b", "J").replaceAll("\\b12\\b", "Q").replaceAll("\\b13\\b", "K").replaceAll("(14|1)", "A") + key.getSecondparam() + " " + String.valueOf(key.getFirstparam1()).replaceAll("\\b10\\b", "T").replaceAll("\\b11\\b", "J").replaceAll("\\b12\\b", "Q").replaceAll("\\b13\\b", "K").replaceAll("(14|1)", "A") + key.getSecondparam1()).queue());
                    for (PokerPlayer player : playersActive) {
                        channel.sendMessage(player.getDiscordTag() + " current Balance:" + player.getBalance()).queue();
                    }
                }
                playersActive.removeIf(player -> player.getBalance() == 0);
                playersIn.clear();
                playersIn.addAll(playersActive);
                betAmounts.clear();
                deckNumbers.clear();
                deckSuits.clear();
                pot = 0;
                for (int i = 0; i < playersActive.size(); i++) {
                    betAmounts.add(i, 0);
                    playersActive.get(i).isFold = false;
                    playersActive.get(i).isAllIn = false;
                }
                cards.clear();
                extraOne = false;
                filling();
                cardPool.delete(0, cardPool.length());
            }
            channel.sendMessage("Absolute Winner is: " + playersActive.get(0).getDiscordTag()).queue();
            isPlaying = false;
            names.clear();
        }
    }

    public static void filling() {
        for (String suit : suits) {
            for (int i = 0; i <= numbers.size() - 1; i++) {
                cards.add(numbers.get(i) + "" + suit);
            }
        }
    }

    public void shuffle(PokerPlayer player) {
        for (int i = 0; i < 2; i++) {
            Random random = new Random();
            Random random1 = new Random();
            int firstparam = numbers.get(random.nextInt(numbers.size()));
            String secondparam = suits.get(random1.nextInt(suits.size()));
            if (cards.contains(firstparam + "" + secondparam)) {
                cards.remove(firstparam + "" + secondparam);
                if (i == 1) {
                    player.setFirstparam1(firstparam);
                    player.setSecondparam1(secondparam);
                } else {
                    player.setFirstparam(firstparam);
                    player.setSecondparam(secondparam);
                }
            } else {
                i--;
            }
        }
    }

    public void shuffle() {
        while (true) {
            Random random = new Random();
            Random random1 = new Random();
            int firstparam = numbers.get(random.nextInt(numbers.size()));
            String secondparam = suits.get(random1.nextInt(suits.size()));
            if (cards.contains(firstparam + "" + secondparam)) {
                if (firstparam == 14 && extraOne) {
                    deckNumbers.add(1);
                    deckSuits.add(secondparam);
                    extraOne = false;
                }
                cards.remove(firstparam + "" + secondparam);
                deckNumbers.add(firstparam);
                deckSuits.add(secondparam);
                cardPool.append(String.valueOf(firstparam).replaceAll("\\b11\\b", "J")
                        .replaceAll("\\b12\\b", "Q")
                        .replaceAll("\\b13\\b", "K").replaceAll("\\b10\\b", "T").replaceAll("(14|1)", "A")).append(secondparam).append(" ");
                break;
            }
        }
    }

    public long maxValueHandEvaluation(ArrayList<Integer> cs, ArrayList<String> suits) {
        ArrayList<Long> possibleValues = new ArrayList<>();
        List<List<Integer>> csSet = new LinkedList<>(combination(cs, 5));
        List<List<String>> suitsSet = new LinkedList<>(combination(suits, 5));
        for (int i = 0; i < csSet.size(); i++) {
            if (!(csSet.get(i).contains(1) && csSet.get(i).contains(14))) {
                possibleValues.add(evaluateHand(csSet.get(i), suitsSet.get(i)));
            }
        }
        Collections.sort(possibleValues);
        Collections.reverse(possibleValues);
        return possibleValues.get(0);
    }
    public static <T> List<List<T>> combination(List<T> values, int size) {

        if (size == 0) {
            return Collections.singletonList(Collections.<T>emptyList());
        }
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<T>> combination = new LinkedList<List<T>>();
        T actual = values.iterator().next();
        List<T> subSet = new LinkedList<T>(values);
        subSet.remove(actual);
        List<List<T>> subSetCombination = combination(subSet, size - 1);
        for (List<T> set : subSetCombination) {
            List<T> newSet = new LinkedList<T>(set);
            newSet.add(0, actual);
            combination.add(newSet);
        }

        combination.addAll(combination(subSet, size));

        return combination;
    }

    public long evaluateHand(List<Integer> cs, List<String> suits) {
        Collections.sort(cs);
        Collections.reverse(cs);
        Map<Integer, Integer> counts = new HashMap<>();
        for (int str : cs) {
            if (counts.containsKey(str)) {
                counts.put(str, counts.get(str) + 1);
            } else {
                counts.put(str, 1);
            }
        }
        ArrayList<Integer> newList = new ArrayList<>();
        int i = 1;
        boolean pair = false;
        boolean set = false;
        int pairInt = 0;
        int setInt = 0;
        boolean a = true;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == 2) {
                if (a) {
                    newList.add(entry.getKey());
                    newList.add(entry.getKey());
                    pairInt = entry.getKey();
                    Collections.sort(newList);
                    Collections.reverse(newList);
                    cs.remove(entry.getKey());
                    cs.remove(entry.getKey());
                    pair = true;
                    i = 10;
                    a = false;
                } else {
                    newList.add(entry.getKey());
                    newList.add(entry.getKey());
                    Collections.sort(newList);
                    Collections.reverse(newList);
                    cs.remove(entry.getKey());
                    cs.remove(entry.getKey());
                    i = 100;
                }
            } else {
                if (entry.getValue() == 3) {
                    newList.add(entry.getKey());
                    newList.add(entry.getKey());
                    newList.add(entry.getKey());
                    Collections.sort(newList);
                    setInt = entry.getKey();
                    Collections.reverse(newList);
                    cs.remove(entry.getKey());
                    cs.remove(entry.getKey());
                    cs.remove(entry.getKey());
                    i = 1000;
                    set = true;
                } else {
                    if (entry.getValue() == 4) {
                        newList.add(entry.getKey());
                        newList.add(entry.getKey());
                        newList.add(entry.getKey());
                        newList.add(entry.getKey());
                        Collections.sort(newList);
                        Collections.reverse(newList);
                        cs.remove(entry.getKey());
                        cs.remove(entry.getKey());
                        cs.remove(entry.getKey());
                        cs.remove(entry.getKey());
                        i = 10000000;
                    }
                }
            }

        }
        if (set && pair) {
            if (setInt < pairInt) {
                Collections.sort(newList);
            }
            newList.addAll(cs);
            i = 1000000;
        }
        newList.addAll(cs);
        int i1 = 1;
        if (new HashSet<>(suits).size() == 1) {
            i1 = 100000;
        }
        int i2 = 10000;
        for (int j = 1; j < 5; ++j) {
            if (newList.get(j) - newList.get(j - 1) != -1) {
                i2 = 1;
                break;
            }
        }
        return (long) (newList.get(0) * 10000 + newList.get(1) * 1000 + newList.get(2) * 100 + newList.get(3) * 10 + newList.get(4)) * i * i1 * i2;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        lastMessage = event.getMessage();
        if (isPlaying) {
            lock.lock();
            try {
                messageReceived.signal();
            } finally {
                lock.unlock();
            }
        }
    }
    @Override
    public void onReady(ReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("startgame", "Begin the game with people in current vc"));
        event.getJDA().updateCommands().addCommands(commandData).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        TextChannel channel = (TextChannel) event.getChannel();
        VoiceChannel connectedChannel = (VoiceChannel) Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel();
        Poker.connectedChannel = connectedChannel;
        Poker.channel = channel;
        channel.getManager().setSlowmode(0).queue();
        playersAmount = connectedChannel.getMembers().size();
        if (event.getName().equals("startgame") && !isPlaying) {
            event.reply("Starting new game...").queue();
            for (Member member : connectedChannel.getMembers()) {
                names.add(member.getUser().getName() + "#" + member.getUser().getDiscriminator());
                ids.add(member.getIdLong());
                isPlaying = true;
            }
            filling();
            channel.sendMessage("-> " + connectedChannel.getName() + " -->> " + names).queue();
            lock.lock();
            try {
                messageReceived.signal();
            } finally {
                lock.unlock();
            }
        }
    }
}


