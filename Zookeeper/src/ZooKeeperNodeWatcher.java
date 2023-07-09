import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ZooKeeperNodeWatcher implements Watcher {
    private final static Logger logger = LoggerFactory.getLogger(ZooKeeperNodeWatcher.class);
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 5000;
    private final String[] exec;
    private final String znode;
    private Process child;
    private ZooKeeper zooKeeper;

    public ZooKeeperNodeWatcher(String znode, String[] exec) {
        this.exec = exec;
        this.znode = znode;
        try {
            zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void watchNode() {
        try {
            Stat nodeStat = zooKeeper.exists(znode, this);
            if (nodeStat != null) {
                logger.info("Node exists!");
                zooKeeper.getChildren(znode, this);
            } else {
                logger.info("Node does not exist!");
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void watchTerminal() throws IOException, InterruptedException, KeeperException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String command = br.readLine();
            if (command.equals("ls")) {
                List<String> children = zooKeeper.getChildren(znode, this);
                printNodeChildren(znode, children, 0);
            }
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeCreated) {
            try {
                child = Runtime.getRuntime().exec(exec);
            } catch (IOException e) {
                logger.error("Failed to execute the command: " + e.getMessage(), e);
            }
            try {
                zooKeeper.getChildren(event.getPath(), this);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }

        } else if (event.getType() == Event.EventType.NodeDeleted) {
            try {
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                    logger.error("Interrupted killing child process", e);
                    System.exit(5);
                }
            } catch (NullPointerException e) {
                logger.error("Can't kill child process which not exist", e);
            }
        } else if (event.getType() == Event.EventType.NodeChildrenChanged) {
            try {
                List<String> children = zooKeeper.getChildren(znode, this);
                logger.info(znode + " has " + children.size() + " children.");
            } catch (KeeperException.NoNodeException e) {
                logger.error("Cannot count children, because node don't exist anymore", e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            zooKeeper.exists(event.getPath(), this);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printNodeChildren(String nodePath, List<String> children, int tabs) throws KeeperException, InterruptedException {
        for (String child : children) {
            String childPath = nodePath + "/" + child;
            List<String> grandchildren = zooKeeper.getChildren(childPath, false);
            StringBuilder childPathToPrint = new StringBuilder(childPath);
            for (int i = 0; i < tabs; i++) childPathToPrint.insert(0, "\t");
            System.out.println(childPathToPrint);
            printNodeChildren(childPath, grandchildren, tabs + 1);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        if (args.length < 2) {
            logger.error("Usage: java ZooKeeperNodeWatcher <znode> <command>");
            System.exit(1);
        }
        String znode = args[0];
        String[] exec = new String[args.length - 1];
        System.arraycopy(args, 1, exec, 0, exec.length);

        ZooKeeperNodeWatcher nodeWatcher = new ZooKeeperNodeWatcher(znode, exec);
        nodeWatcher.watchNode();
        nodeWatcher.watchTerminal();

    }
}
