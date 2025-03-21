package com.example.goforit;

import java.util.*;

public class Maze {

    private int size;
    private boolean[][] obstacles;
    private Set<Point> path;
    private Point goal;
    private Point start;

    public Maze() {}

    public void generateMaze(int size, int maxDirectionChanges) {
        while (true) {
            this.size = size;
            obstacles = new boolean[size + 2][size + 2];
            path = new LinkedHashSet<>();

            for (int i = 0; i < size + 2; i++) {
                obstacles[0][i] = true;
                obstacles[size + 1][i] = true;
                obstacles[i][0] = true;
                obstacles[i][size + 1] = true;
            }

            Random rand = new Random();
            start = new Point(rand.nextInt(size) + 1, rand.nextInt(size) + 1);

            Point current = new Point(start.x, start.y);
            path.add(new Point(current.x, current.y));
            obstacles[current.x][current.y] = false;

            int directionChanges = 0;
            Point prevDirection = null;

            while (directionChanges < maxDirectionChanges) {
                List<int[]> possibleDirs = new ArrayList<>(Arrays.asList(
                        new int[]{0, 1}, new int[]{1, 0}, new int[]{0, -1}, new int[]{-1, 0}
                ));
                Collections.shuffle(possibleDirs);
                boolean moved = false;

                for (int[] dir : possibleDirs) {
                    if (prevDirection != null && dir[0] == -prevDirection.x && dir[1] == -prevDirection.y) {
                        continue; // Empêcher le retour en arrière
                    }

                    int dx = dir[0];
                    int dy = dir[1];
                    int maxDist = getMaxDistance(current, dx, dy);
                    if (maxDist <= 0) continue;

                    int dist = rand.nextInt(maxDist) + 1;
                    List<Point> segment = new ArrayList<>();
                    boolean valid = true;

                    for (int i = 1; i <= dist; i++) {
                        Point step = new Point(current.x + dx * i, current.y + dy * i);
                        if (path.contains(step) || obstacles[step.x][step.y]) {
                            valid = false;
                            break;
                        }
                        segment.add(step);
                    }

                    if (valid) {
                        path.addAll(segment);
                        current = segment.get(segment.size() - 1);
                        if (current.x + dx >= 0 && current.x + dx < size + 2 &&
                                current.y + dy >= 0 && current.y + dy < size + 2) {
                            obstacles[current.x + dx][current.y + dy] = true;
                        }
                        moved = true;
                        prevDirection = new Point(dx, dy);
                        directionChanges++;
                        break;
                    }
                }

                if (!moved) break;
            }

            if (directionChanges < maxDirectionChanges) continue; // Recommencer

            placeExit(current, prevDirection);
            break;
        }
    }


    private int getMaxDistance(Point current, int dx, int dy) {
        int distance = 0;
        int x = current.x;
        int y = current.y;

        while (x + dx > 0 && x + dx < size + 1 && y + dy > 0 && y + dy < size + 1) {
            x += dx;
            y += dy;
            if (obstacles[x][y]) break;
            distance++;
        }
        return distance > 0 ? distance : 1;
    }

    private void placeExit(Point lastPoint, Point lastDirection) {
        List<Point> possibleExits = new ArrayList<>();
        int[][] dirs = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        for (int[] dir : dirs) {
            if (dir[0] == -lastDirection.x && dir[1] == -lastDirection.y) {
                continue; // Éviter de revenir en arrière pour la sortie
            }

            int dx = dir[0];
            int dy = dir[1];
            Point check = new Point(lastPoint.x, lastPoint.y);
            boolean clear = true;

            while (check.x > 0 && check.x < size + 1 && check.y > 0 && check.y < size + 1) {
                if (obstacles[check.x][check.y]) {
                    clear = false;
                    break;
                }
                check = new Point(check.x + dx, check.y + dy);
            }

            if (clear && (check.x == 0 || check.x == size + 1 || check.y == 0 || check.y == size + 1) && !(check.x == 1 && (check.y == 1 || check.y == size)) && !(check.x == size && (check.y == 1 || check.y == size)) && !(check.y == 1 && (check.x == 1 || check.x == size)) && !(check.y == size && (check.x == 1 || check.x == size))) {
                possibleExits.add(check);
            }
        }

        if (!possibleExits.isEmpty()) {
            goal = possibleExits.get(new Random().nextInt(possibleExits.size()));
            obstacles[goal.x][goal.y] = false;
        }
    }

    public boolean isPartOfPath(int x, int y) {
        return path.contains(new Point(x, y)) && !isObstacle(x, y);
    }

    public boolean isGoal(int x, int y) {
        return goal.x == x && goal.y == y;
    }

    public boolean isObstacle(int x, int y) {
        return obstacles[x][y];
    }

    public boolean isStart(int x, int y) {
        return start != null && start.x == x && start.y == y;
    }

    public Point getStart() {
        return start;
    }
    public int getSize() {
        return size;
    }
}
