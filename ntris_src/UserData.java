package ntris_src;

public class UserData implements Comparable {
    public static final int ONLINE = 0;
    public static final int SEEKING = 1;
    public static final int INGAME = 2;

    public final String name;
    public int status;

    public UserData(String n) {
        name = n;
        status = ONLINE;
    }

    public UserData(String n, boolean parse) {
        status = ONLINE;

        if (parse) {
            String[] tokens = n.split(" ");
            name = tokens[0];

            if (tokens.length > 1) {
                status = SEEKING;
                if (tokens[1].equals("(playing"))
                    status = INGAME;
            }
        } else {
            name = n;
        }
    }

    public UserData(String n, int s) {
        name = n;
        status = s;
    }

    public boolean equals(Object other) {
        if (other instanceof UserData)
            return name.equals(((UserData)other).name);
        return false;
    }

    public int compareTo(Object other) {
        if (other instanceof UserData)
            return name.compareTo(((UserData)other).name);
        return 0;
    }

    public String toString() {
        switch (status) {
            case SEEKING:
                //return "<html><b>" + name + "</b></html>";
                return name + " - wants a game";
            case INGAME:
                //return "<html><i>" + name + "</i></html>";
                return name + " - playing";
            default:
                return name;
        }
    }
}
