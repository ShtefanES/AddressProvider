package ru.neoanon.addressprovider;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.geo.Projection;
import com.yandex.mapkit.geometry.geo.Projections;
import com.yandex.mapkit.geometry.geo.XYPoint;

import java.util.ArrayList;
import java.util.List;

public class RegionHelper {
    private static final Point POINT_1 = new Point(53.462044, 83.754043);
    private static final Point POINT_2 = new Point(53.274362, 83.942184);
    private static final Point POINT_3 = new Point(53.204333, 83.660660);
    private static final Point POINT_4 = new Point(53.421457, 83.514404);

    private static Projection projection = Projections.createWgs84Mercator();

    List<XYPoint> points;

    public RegionHelper() {
        initPoints();
    }

    /**
     * Return true if the given point is contained inside the boundary.
     * See: http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
     *
     * @param testPoint The point to check
     * @return true if the point is inside the boundary, false otherwise
     */
    public boolean checkCityContainPoint(Point testPoint) {
        XYPoint test = projection.worldToXY(testPoint, MapFragment.COMFORTABLE_ZOOM_LEVEL);
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            if ((points.get(i).getY() > test.getY()) != (points.get(j).getY() > test.getY()) &&
                    (test.getX() < (points.get(j).getX() - points.get(i).getX()) * (test.getY() - points.get(i).getY()) / (points.get(j).getY() - points.get(i).getY()) + points.get(i).getX())) {
                result = !result;
            }
        }
        return result;
    }

    private void initPoints() {
        points = new ArrayList<>();
        points.add(projection.worldToXY(POINT_1, MapFragment.COMFORTABLE_ZOOM_LEVEL));
        points.add(projection.worldToXY(POINT_2, MapFragment.COMFORTABLE_ZOOM_LEVEL));
        points.add(projection.worldToXY(POINT_3, MapFragment.COMFORTABLE_ZOOM_LEVEL));
        points.add(projection.worldToXY(POINT_4, MapFragment.COMFORTABLE_ZOOM_LEVEL));
    }
}
