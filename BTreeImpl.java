import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class BTreeImpl {

    static final boolean DEBUG = false;
    static int NODE_ARITY = 12;
    static int OFFSET = 16;
    static int order = NODE_ARITY;
    static int NODE_POINTER_EMPTY = 8;
    static int KEY_LENGTH = 32;
    static int KEY_SIZE = KEY_LENGTH * NODE_ARITY;
    static int VALUE_LENGTH = 293;
    static int VALUE_SIZE = VALUE_LENGTH * NODE_ARITY;
    static int PARENT_SIZE = 8;
    static int NODE_SIZE = 4096;
    static int NUM_ELEMENTS_SIZE = 4;
    public Element root;
    public int TREE_SIZE = 0;
    public LinkedList<Long> emptyNodes = new LinkedList<Long>();
    public Map<String, Element> nodeCache;
    public boolean loading = false;
    static int PAD = NODE_SIZE - PARENT_SIZE - NUM_ELEMENTS_SIZE - KEY_SIZE - VALUE_SIZE;
    RandomAccessFile file;
    int removed = 0;

    public BTreeImpl(RandomAccessFile file) {
        try {
            this.file = file;
            if (file.length() == 0) {
                file.seek(0);
                file.writeInt(TREE_SIZE);
                file.writeInt(NODE_ARITY);
                file.writeLong(-1);
            } else {
                root = new Element();
                root = root.readElement(OFFSET);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

		Map HashMapLink = new LinkedHashMap<String, Element>(200) {

            private static final int ENTRIES = 1000;

            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > ENTRIES;
            }
        };
        nodeCache = HashMapLink;
    }

    public boolean add(String key, String data) {

        if (key == null) {
            return false;
        }

        try {
            if (file.length() <= OFFSET) {
                root = new Element(key, data);
                root.parent = -1;
                file.seek(OFFSET);
                root.writeInfo(root, -1);
            } else {
                boolean isSplitNeeded = root.set(key, data);

                if (isSplitNeeded) {
                    root.writeInfo(root, -1);
                    root.breakRoot();
                }

                file.seek(OFFSET);
                root.writeInfo(root, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        this.TREE_SIZE++;
        return true;
    }

    private class Element {

        List<String> values;
        List<String> keys;
        long parent;

        public Element(String key, String value) {
            this.values = new LinkedList();
            this.keys = new LinkedList();
            this.values.add(value);
            this.keys.add(key);
        }

        public Element() {
            this.parent = -1;
            this.values = new LinkedList();
            this.keys = new LinkedList();
        }


        public long removeEmptyNodePointer() throws Exception {
            file.seek(NODE_POINTER_EMPTY);
            long emptyAddress = file.readLong();

            if (emptyAddress != -1) {
                Element emptyNode = fetchElement(emptyAddress);
                String firstValue = emptyNode.values.get(0);
                long nextemptyAddress;

                if (!firstValue.equals("$END")) {
                    nextemptyAddress = extractPointerVal(firstValue);
                } else {
                    nextemptyAddress = -1;
                }
                ;

                file.seek(NODE_POINTER_EMPTY);
                file.writeLong(nextemptyAddress);
            }

            return emptyAddress;
        }

        public void writeInfo(Element element, long currentSeek) {
            try {
                if (element.parent == -1 || currentSeek == -1) {
                    file.seek(OFFSET);
                    file.writeLong(-1);
                } else {
                    file.seek(currentSeek);
                    file.writeLong(element.parent);
                }
                int elementKeyCount = element.keys.size();
                file.writeInt(elementKeyCount);
                for (String key : element.keys) {
                    writePaddedInfo(key, KEY_LENGTH);
                }
                writePadding((NODE_ARITY - elementKeyCount) * KEY_LENGTH);
                for (String value : element.values) {
                    writePaddedInfo(value, VALUE_LENGTH);
                }
                writePadding((NODE_ARITY - elementKeyCount) * VALUE_LENGTH);

                writePadding(PAD);

                nodeCache.put(currentSeek + "", element);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void writePaddedInfo(String data, int length) throws IOException {
            byte[] bytes = data.getBytes();
            file.write(bytes);

            int paddingLength = length - bytes.length;
            writePadding(paddingLength);
        }

        private void writePadding(int length) throws IOException {
            byte[] padding = new byte[length];

            Arrays.fill(padding, (byte) ' ');
            file.write(padding);
        }

        public Element readElement(long filePointer) {
            try {
                Element element = new Element();
                file.seek(filePointer);
                element.parent = file.readLong();
                int elementCount = file.readInt();
                long keyStartPosition = file.getFilePointer();

                for (int i = 0; i < elementCount; i++) {
                    byte[] keyBytes = new byte[KEY_LENGTH];
                    file.readFully(keyBytes, 0, KEY_LENGTH);
                    String key = new String(keyBytes).trim();
                    element.keys.add(key);
                }

                long valueStartPosition = keyStartPosition + KEY_SIZE;
                file.seek(valueStartPosition);

                for (int i = 0; i < elementCount; i++) {
                    byte[] valueBytes = new byte[VALUE_LENGTH];
                    file.readFully(valueBytes, 0, VALUE_LENGTH);
                    String value = new String(valueBytes).trim();
                    element.values.add(value);
                }
                return element;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public Element fetchElement(long address) {
            Element n;
            if (nodeCache.containsKey(address + "")) {
                n = nodeCache.get(address + "");
            } else {
                n = this.readElement(address);
            }
            return n;
        }

        public boolean hasChildNodes() {
            if (this.values.size() == 0) {
                return false;
            } else {
                return this.values.get(0).startsWith("$");
            }
        }

        public void breakRoot() {
            boolean hasChildren = hasChildNodes();
            boolean shouldSetRight = true;
            Element left_Element = new Element();
            Element right_Element = new Element();

            // simplify the methods used to obtain the pointers
            long left_ElementPointer = NextAvailablePointer();
            nodeCache.put(String.valueOf(left_ElementPointer), left_Element);
            long right_ElementPointer = NextAvailablePointer();
            nodeCache.put(String.valueOf(right_ElementPointer), right_Element);
            int midpoint = (int) Math.ceil(1.0 * BTreeImpl.order / 2);
            int keysSize = this.keys.size();
            // refactor loop: use iteration on list instead of for loop
            for (int i = 0; i < keysSize; i++) {
                if (i < midpoint) {
                    left_Element.set(this.keys.remove(0), this.values.remove(0));
                } else {
                    right_Element.set(this.keys.remove(0), this.values.remove(0));
                }
            }
            // if it has children, set the flag to false and modify the left_Element's last key
            if (hasChildren) {
                shouldSetRight = false;
                String createdLeftPointerLocation = formatPointerVal(left_ElementPointer);
                this.set(left_Element.keys.set(left_Element.keys.size() - 1, "null"), createdLeftPointerLocation);
            }
            // set the parents for the new elements
            left_Element.parent = OFFSET;
            right_Element.parent = OFFSET;
            if (shouldSetRight) {
                String createdLeftPointerLocation = formatPointerVal(left_ElementPointer);
                this.set(right_Element.keys.get(0), createdLeftPointerLocation);
            }
            // create the pointer location for the right_Element
            String createdRightPointerLocation = formatPointerVal(right_ElementPointer);
            this.set("null", createdRightPointerLocation);
            // write data for the new elements
            writeInfo(left_Element, left_ElementPointer);
            writeInfo(right_Element, right_ElementPointer);
        }

        private String formatPointerVal(long pointerValue) {
            return "$" + pointerValue;
        }

        private long extractPointerVal(String formattedPointer) {
            if (formattedPointer.startsWith("$")) {
                return Long.parseLong(formattedPointer.substring(1));
            } else {
                return -99;
            }
        }

        private long NextAvailablePointer() {
            try {
                long nextPointer = this.removeEmptyNodePointer();
                if (nextPointer == -1) {
                    long fileLength = file.length();
                    String keyFileLength = Long.toString(fileLength);
                    while (nodeCache.containsKey(keyFileLength)) {
                        fileLength += NODE_SIZE;
                        keyFileLength = Long.toString(fileLength);
                    }
                    return fileLength;
                } else {
                    return nextPointer;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                return -1;
            }
        }

        private void breakLeafNode(Element right) {
            Element left = new Element();
            long leftPointer = this.NextAvailablePointer();
            nodeCache.put(leftPointer + "", left);
            int half = (int) Math.ceil(1.0 * BTreeImpl.order / 2);
            for (int i = 0; i < half; i++) {
                left.set(right.keys.remove(0), right.values.remove(0));
            }
            this.set(right.keys.get(0), this.formatPointerVal(leftPointer));
            this.writeInfo(left, leftPointer);
        }

        private void breakInternalNode(Element right) {
            Element left = new Element();
            long leftPointer = this.NextAvailablePointer();
            nodeCache.put(leftPointer + "", left);
            int half = (int) Math.ceil(1.0 * BTreeImpl.order / 2);
            for (int i = 0; i < half; i++) {
                left.set(right.keys.remove(0), right.values.remove(0));
            }
            String s = left.keys.set(left.keys.size() - 1, "null");
            this.set(s, this.formatPointerVal(leftPointer));
            this.writeInfo(left, leftPointer);
        }

        private boolean needToSplit() {
            return this.keys.size() > BTreeImpl.order - 1;
        }

        private void splitChild(Element n) {
            if (!n.hasChildNodes()) {
                this.breakLeafNode(n);
            } else {
                this.breakInternalNode(n);
            }
        }

        public boolean set(String key, String value) {
            if (keys.isEmpty()) {
                keys.add(key);
                values.add(value);
                return needToSplit();
            }

            if (!hasChildNodes()) {
                return addKeyAndValueInNonChildNodes(key, value);
            } else {
                return addKeyAndValueInChildNodes(key, value);
            }
        }

        private boolean addKeyAndValueInNonChildNodes(String key, String value) {
            for (int i = 0; i < keys.size(); i++) {
                int comparisonResult = keys.get(i).compareTo(key);
                if (key.equals("null") || key.equals(keys.get(i)) || comparisonResult < 0) {
                    keys.add(i, key);
                    values.add(i, value);
                    return needToSplit();
                }
            }
            keys.add(key);
            values.add(value);
            return needToSplit();
        }

        private boolean addKeyAndValueInChildNodes(String key, String value) {
            if (value.startsWith("$")) {
                return handleKeyAndValueStartingWithDollar(key, value);
            } else {
                return addKeyAndValueInRealChildNodes(key, value);
            }
        }

        private boolean handleKeyAndValueStartingWithDollar(String key, String value) {
            for (int i = 0; i < keys.size(); i++) {
                if (!keys.get(i).equals("null")) {
                    int comparisonResult = keys.get(i).compareTo(key);
                    if (key.equals(keys.get(i)) || comparisonResult < 0) {
                        keys.add(i, key);
                        values.add(i, value);
                        return needToSplit();
                    }
                }
            }

            if (keys.contains("null")) {
                keys.add(keys.size() - 1, key);
                values.add(values.size() - 1, value);
                return needToSplit();
            }

            keys.add(key);
            values.add(value);
            return needToSplit();
        }

        private boolean addKeyAndValueInRealChildNodes(String key, String value) {
            for (int i = 0; i < keys.size() - 1; i++) {
                int comparison = this.keys.get(i).compareTo(key);
                if (key.equals(keys.get(i)) || comparison < 0) {
                    return processChildNode(i, key, value);
                }
            }
            return processLastChildNode(key, value);
        }

        private boolean processChildNode(int index, String key, String value) {
            long pointerLocation = extractPointerVal(values.get(index));
            Element node = fetchElement(pointerLocation);
            if (node.set(key, value)) {
                splitChild(node);
            }
            writeInfo(node, pointerLocation);
            return needToSplit();
        }

        private boolean processLastChildNode(String key, String value) {
            long pointerLocation = extractPointerVal(values.get(keys.size() - 1));
            Element node = fetchElement(pointerLocation);
            if (node.set(key, value)) {
                splitChild(node);
            }
            writeInfo(node, pointerLocation);
            return needToSplit();
        }
    }
}