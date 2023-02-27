import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Poker extends ListenerAdapter{
    static JDA jda;
    static ArrayList<String> cards = new ArrayList<>();
    static ArrayList<String> suits = new ArrayList<>();
    static ArrayList<Integer> numbers = new ArrayList<>();
    static ArrayList<Integer> deckNumbers = new ArrayList<>();
    static ArrayList<String> deckSuits = new ArrayList<>();
    static HashMap<PokerPlayer, Long> playerValues = new HashMap<>();
    static boolean extraOne = true;
    static int pot=0;
    static ArrayList<String> names = new ArrayList<>();
    static ArrayList<Long> ids = new ArrayList<>();
    static TextChannel channel;
    static VoiceChannel connectedChannel;
    static boolean isPlaying=false;
    static int playersAmount;
    static Message lastMessage;
    private static final Lock lock = new ReentrantLock();
    private static final Condition messageReceived = lock.newCondition();
    public static void main(String[] args) throws InterruptedException {
        jda = JDABuilder.createDefault("MTA1ODQ2NTM3Njg5MzE0NTIxMQ.GSHqq-.YZp3Z7HpeyhnUxpYANYBHDKuJkFkfSlPNV0vBI")
                .enableIntents(GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGE_TYPING,
                        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                        GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.playing("дифиченто"))
                .addEventListeners(new Poker())
                .build();
        jda.awaitReady();
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
                playersAmount = connectedChannel.getMembers().size();
                ArrayList<PokerPlayer> playersIn = new ArrayList<>();
                ArrayList<PokerPlayer> playersActive = new ArrayList<>(playersIn);
                for (int i = 0; i < playersAmount; i++) {
                    PokerPlayer pokerPlayer = new PokerPlayer(10000, 1, "a", 1, "a", names.get(i), ids.get(i));
                    playersIn.add(pokerPlayer);
                    new Poker().shuffle(pokerPlayer);
                }
                for (PokerPlayer player : playersIn) {
                    User user = Poker.jda.retrieveUserById(player.getId()).complete();
                    user.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessage(player.getFirstparam() + player.getSecondparam() + " " + player.getFirstparam1() + player.getSecondparam1())).queue();
                }
                // дебаг:
                // поставить breakpoint у той строчки с которой начинать проверку
                // нет действий при олл-ине
                // сделать малые и большие блайнды
                ArrayList<Integer> betAmounts = new ArrayList<>(playersAmount);
                for (int i = 0; i < playersIn.size(); i++) {
                    betAmounts.add(i, 0);
                }
                playersActive.addAll(playersIn);
                while (playersActive.size() != 1) {
                    boolean onePlayerLeft = false;
                    int lastRoundBet = 0;
                    do {
                        for (int i = 0; i < playersIn.size(); i++) {
                            if (!playersIn.get(i).isFold) {
                                lock.lock();
                                try {
                                    messageReceived.await();
                                } finally {
                                    lock.unlock();
                                }
                                System.out.println(lastMessage.getContentRaw());
                               if ((lastMessage.getAuthor().getName() + "#" + lastMessage.getAuthor().getDiscriminator()).equals(playersIn.get(i).getDiscordTag())) {
                                   String betAmount1 = lastMessage.getContentRaw();
                                   if (betAmount1.equals("fold")) {
                                       channel.sendMessage("Вы вышли из игры").queue();
                                       playersIn.get(i).isFold = true;
                                   } else {
                                       try {
                                           if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) >= 0) {
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
                                               channel.sendMessage("Нет средств").queue();
                                               i--;
                                           }
                                       } catch (NumberFormatException e) {
                                           i--;
                                       }

                                       for (int j = 0; j < playersIn.size(); j++) {
                                           if (playersIn.get(j).isFold) {
                                               betAmounts.remove(j);
                                               playersIn.remove(playersIn.get(j));
                                               i--;
                                           }
                                       }
                                       channel.sendMessage(betAmounts.toString()).queue();
                                       if (playersIn.size() == 1) {
                                           onePlayerLeft = true;
                                       }
                                       if (new HashSet<>(betAmounts).size() == 1 && !(new HashSet<>(betAmounts).size() == 1 && new HashSet<>(betAmounts).contains(lastRoundBet))) {
                                           channel.sendMessage("Current pot: " + pot).queue();
                                           channel.sendMessage(betAmounts.toString()).queue();
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
                    if (!onePlayerLeft) {
                        do {
                            for (int i = 0; i < playersIn.size(); i++) {
                                if (!playersIn.get(i).isFold) {
                                    lock.lock();
                                    try {
                                        messageReceived.await();
                                    } finally {
                                        lock.unlock();
                                    }
                                    System.out.println(lastMessage.getContentRaw());
                                    if ((lastMessage.getAuthor().getName() + "#" + lastMessage.getAuthor().getDiscriminator()).equals(playersIn.get(i).getDiscordTag())) {
                                        String betAmount1 = lastMessage.getContentRaw();
                                        if (betAmount1.equals("fold")) {
                                            channel.sendMessage("Вы вышли из игры").queue();
                                            playersIn.get(i).isFold = true;
                                        } else {
                                            try {
                                                if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) >= 0) {
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
                                                    channel.sendMessage("Нет средств").queue();
                                                    i--;
                                                }
                                            } catch (NumberFormatException e) {
                                                i--;
                                            }
                                        }
                                        for (int j = 0; j < playersIn.size(); j++) {
                                            if (playersIn.get(j).isFold) {
                                                betAmounts.remove(j);
                                                playersIn.remove(playersIn.get(j));
                                                i--;
                                            }
                                        }
                                        channel.sendMessage(betAmounts.toString()).queue();
                                        if (playersIn.size() == 1) {
                                            onePlayerLeft = true;
                                        }
                                        if (new HashSet<>(betAmounts).size() == 1 && !(new HashSet<>(betAmounts).size() == 1 && new HashSet<>(betAmounts).contains(lastRoundBet))) {
                                            channel.sendMessage("Current pot: " + pot).queue();
                                            channel.sendMessage(betAmounts.toString()).queue();
                                            break;
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
                    if (!onePlayerLeft) {
                        do {
                            for (int i = 0; i < playersIn.size(); i++) {
                                if (!playersIn.get(i).isFold) {
                                    lock.lock();
                                    try {
                                        messageReceived.await();
                                    } finally {
                                        lock.unlock();
                                    }
                                    System.out.println(lastMessage.getContentRaw());
                                    if ((lastMessage.getAuthor().getName() + "#" + lastMessage.getAuthor().getDiscriminator()).equals(playersIn.get(i).getDiscordTag())) {
                                        String betAmount1 = lastMessage.getContentRaw();
                                        if (betAmount1.equals("fold")) {
                                            channel.sendMessage("Вы вышли из игры").queue();
                                            playersIn.get(i).isFold = true;
                                        } else {
                                            try {
                                                if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) >= 0) {
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
                                                    channel.sendMessage("Нет средств").queue();
                                                    i--;
                                                }
                                            } catch (NumberFormatException e) {
                                                i--;
                                            }
                                        }
                                        for (int j = 0; j < playersIn.size(); j++) {
                                            if (playersIn.get(j).isFold) {
                                                betAmounts.remove(j);
                                                playersIn.remove(playersIn.get(j));
                                                i--;
                                            }
                                        }
                                        channel.sendMessage(betAmounts.toString()).queue();
                                        if (playersIn.size() == 1) {
                                            onePlayerLeft = true;
                                        }
                                        if (new HashSet<>(betAmounts).size() == 1 && !(new HashSet<>(betAmounts).size() == 1 && new HashSet<>(betAmounts).contains(lastRoundBet))) {
                                            channel.sendMessage("Current pot: " + pot).queue();
                                            channel.sendMessage(betAmounts.toString()).queue();
                                            break;
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
                    if (!onePlayerLeft) {
                        do {
                            for (int i = 0; i < playersIn.size(); i++) {
                                if (!playersIn.get(i).isFold) {
                                    lock.lock();
                                    try {
                                        messageReceived.await();
                                    } finally {
                                        lock.unlock();
                                    }
                                    System.out.println(lastMessage.getContentRaw());
                                    if ((lastMessage.getAuthor().getName() + "#" + lastMessage.getAuthor().getDiscriminator()).equals(playersIn.get(i).getDiscordTag())) {
                                        String betAmount1 = lastMessage.getContentRaw();
                                        if (betAmount1.equals("fold")) {
                                            channel.sendMessage("Вы вышли из игры").queue();
                                            playersIn.get(i).isFold = true;
                                        } else {
                                            try {
                                                if (playersIn.get(i).getBalance() - Integer.parseInt(betAmount1) >= 0) {
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
                                                    channel.sendMessage("Нет средств").queue();
                                                    i--;
                                                }
                                            } catch (NumberFormatException e) {
                                                i--;
                                            }
                                        }
                                        for (int j = 0; j < playersIn.size(); j++) {
                                            if (playersIn.get(j).isFold) {
                                                betAmounts.remove(j);
                                                playersIn.remove(playersIn.get(j));
                                                i--;
                                            }
                                        }
                                        channel.sendMessage(betAmounts.toString()).queue();
                                        if (playersIn.size() == 1) {
                                            onePlayerLeft = true;
                                        }
                                        if (new HashSet<>(betAmounts).size() == 1 && !(new HashSet<>(betAmounts).size() == 1 && new HashSet<>(betAmounts).contains(lastRoundBet))) {
                                            channel.sendMessage("Current pot: " + pot).queue();
                                            channel.sendMessage(betAmounts.toString()).queue();
                                            break;
                                        }
                                    } else {
                                        i--;
                                    }
                                }
                            }
                        }
                        while (new HashSet<>(betAmounts).size() != 1);
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
                        // поправить вывод
                        LinkedHashMap<PokerPlayer, Long> reverseSortedMap = new LinkedHashMap<>();
                        playerValues.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
                        Map.Entry<PokerPlayer, Long> firstEntry = reverseSortedMap.entrySet().iterator().next();
                        firstEntry.getKey().setBalance(firstEntry.getKey().getBalance() + pot);
                        channel.sendMessage(reverseSortedMap.toString()).queue();
                    } else {
                        playersIn.get(0).setBalance(playersIn.get(0).getBalance() + pot);
                    }
                    for (PokerPlayer player : playersIn) {
                        if (player.getBalance() == 0) {
                            playersActive.remove(player);
                        }
                    }
                    playersIn = playersActive;
                    betAmounts.clear();
                    deckNumbers.clear();
                    deckSuits.clear();
                    pot = 0;
                    for (int i = 0; i < playersActive.size(); i++) {
                        betAmounts.add(i, 0);
                        playersActive.get(i).isFold = false;
                    }
                    for (PokerPlayer pokerPlayer : playersActive) {
                        new Poker().shuffle(pokerPlayer);
                        User user = Poker.jda.retrieveUserById(pokerPlayer.getId()).complete();
                        user.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessage(pokerPlayer.getFirstparam() + pokerPlayer.getSecondparam() + " " + pokerPlayer.getFirstparam1() + pokerPlayer.getSecondparam1())).queue();
                    }
                    cards.clear();
                    filling();
                }
                channel.sendMessage("Winner is: " + playersActive.get(0).getDiscordTag()).queue();
                isPlaying = false;
                names.clear();
            }
    }

    public static void filling() {
        suits.add(0, "♠");
        suits.add(1, "♣");
        suits.add(2, "♥");
        suits.add(3, "♦");
        for (int i = 2; i <= 14; i++) {
            numbers.add(i);
        }
        for (String suit : suits) {
            for (int i = 0; i <= numbers.size() - 1; i++) {
                cards.add(numbers.get(i) + "" + suit);
            }
        }
    }
    public void shuffle(PokerPlayer player){
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
                channel.sendMessage(firstparam + secondparam + " ").queue();
                break;
            }
        }
    }

    public long maxValueHandEvaluation(ArrayList<Integer> cs, ArrayList<String> suits) {
        ArrayList<Long> possibleValues = new ArrayList<>();
        List<List<Integer>> csSet = new LinkedList<>(combination(cs, 5));
        List<List<String>> suitsSet = new LinkedList<>(combination(suits, 5));
        for(int i=0;i<csSet.size();i++) {
            if(!(csSet.get(i).contains(1)&&csSet.get(i).contains(14))) {
                possibleValues.add(evaluateHand(csSet.get(i), suitsSet.get(i)));
            }
        }
        Collections.sort(possibleValues);
        Collections.reverse(possibleValues);
        return possibleValues.get(0);
    }


    public static <T> List<List<T>> combination(List<T> values, int size) {

        if (size == 0) {
            return Collections.singletonList(Collections.<T> emptyList());
        }
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<T>> combination = new LinkedList<List<T>>();
        T actual = values.iterator().next();
        List<T> subSet = new LinkedList<T>(values);
        subSet.remove(actual);
        List<List<T>> subSetCombination = combination(subSet, size-1);
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
        int pairint = 0;
        int setint = 0;
        boolean a = true;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == 2) {
                if (a) {
                    newList.add(entry.getKey());
                    newList.add(entry.getKey());
                    pairint = entry.getKey();
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
                    setint = entry.getKey();
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
            if (setint < pairint) {
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
        TextChannel channel = (TextChannel) event.getChannel();
       VoiceChannel connectedChannel = (VoiceChannel) Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel();
        Poker.connectedChannel = connectedChannel;
        Poker.channel = channel;
        channel.getManager().setSlowmode(0).queue();
        playersAmount = connectedChannel.getMembers().size();
        if (event.getMessage().getContentRaw().equals("!startgame") && !isPlaying) {
            for (Member member : connectedChannel.getMembers()) {
                names.add(member.getUser().getName() + "#" + member.getUser().getDiscriminator());
                ids.add(member.getIdLong());
                isPlaying = true;
            }
            filling();
            channel.sendMessage("-> " + connectedChannel.getName() + " -->> " + names).queue();
        } else {
            lastMessage = event.getMessage();
            System.out.println(event.getMessage().getContentRaw());
        }
        lock.lock();
        try {
            messageReceived.signal();
        } finally {
            lock.unlock();
        }
    }
}


