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
                        continue;
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

                        Point obstaclePoint = new Point(current.x + dx, current.y + dy);
                        if (obstaclePoint.x > 0 && obstaclePoint.x < size + 1 &&
                                obstaclePoint.y > 0 && obstaclePoint.y < size + 1 &&
                                !path.contains(obstaclePoint)) {

                            obstacles[obstaclePoint.x][obstaclePoint.y] = true;
                        }

                        moved = true;
                        prevDirection = new Point(dx, dy);
                        directionChanges++;
                        break;
                    }

                }

                if (!moved) break;
            }

            if (directionChanges < maxDirectionChanges) continue;

            placeExit(current, prevDirection);
            placeExtraObstacles();
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
        Random rand = new Random();

        Point chosenExit = null;
        List<Point> exitPath = new ArrayList<>();

        for (int[] dir : dirs) {
            if (dir[0] == -lastDirection.x && dir[1] == -lastDirection.y) {
                continue;
            }

            int dx = dir[0];
            int dy = dir[1];
            Point check = new Point(lastPoint.x, lastPoint.y);
            List<Point> tempPath = new ArrayList<>();

            while (check.x > 0 && check.x < size + 1 && check.y > 0 && check.y < size + 1) {
                check = new Point(check.x + dx, check.y + dy);
                if (obstacles[check.x][check.y]) break;
                tempPath.add(new Point(check.x, check.y));
            }

            if ((check.x == 0 || check.x == size + 1 || check.y == 0 || check.y == size + 1) &&
                    !(check.x == 1 && (check.y == 1 || check.y == size)) &&
                    !(check.x == size && (check.y == 1 || check.y == size)) &&
                    !(check.y == 1 && (check.x == 1 || check.x == size)) &&
                    !(check.y == size && (check.x == 1 || check.x == size))) {

                possibleExits.add(check);

                if (chosenExit == null || rand.nextBoolean()) {
                    chosenExit = check;
                    exitPath = new ArrayList<>(tempPath);
                }
            }
        }

        if (chosenExit != null) {
            goal = chosenExit;
            obstacles[goal.x][goal.y] = false;
            path.addAll(exitPath);
        } else {
            System.out.println(" Aucune sortie valide trouvée, re-génération nécessaire !");
        }
    }

    private void placeExtraObstacles() {
        Random rand = new Random();
        int obstacleCount = (size * size) / 5;

        for (int i = 0; i < obstacleCount; i++) {
            int x, y;
            do {
                x = rand.nextInt(size) + 1;
                y = rand.nextInt(size) + 1;
            } while (path.contains(new Point(x, y)) || obstacles[x][y]);

            obstacles[x][y] = true;
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
    

    /**
     * Récupère le point d'arrivée du labyrinthe
     * @return Le point représentant la sortie
     */
    public Point getGoal() {
        return goal;
    }
    
    public int getSize() {
        return size;
    }

    public Set<Point> getPath() {
        return path;
    }
}
