package dao;

import db.DatabaseManager;
import model.entity.BestSubjectArea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BestSubjectAreaDao {

    public List<BestSubjectArea> findAll() {
        String sql = """
                SELECT best_area_id, area_name
                FROM best_subject_areas
                ORDER BY area_name
                """;

        List<BestSubjectArea> areas = new ArrayList<>();

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                areas.add(new BestSubjectArea(
                        resultSet.getInt("best_area_id"),
                        resultSet.getString("area_name")
                ));
            }

            return areas;

        } catch (Exception exception) {
            throw new RuntimeException("Could not load best subject areas.", exception);
        }
    }
}