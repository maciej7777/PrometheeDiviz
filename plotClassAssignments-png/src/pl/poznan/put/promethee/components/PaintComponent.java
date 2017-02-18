package pl.poznan.put.promethee.components;

import pl.poznan.put.promethee.xmcda.InputsHandler;

import javax.imageio.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.*;
import java.awt.*;
import java.io.*;

/**
 * Created by Maciej Uniejewski on 2016-12-30.
 */
public class PaintComponent {

    private static int width = 1;
    private static int height = 1;

    private PaintComponent() {

    }

    public static byte[] drawImage(InputsHandler.Inputs inputs) {
        byte[] bytes = null;

        int categoriesNumber = inputs.getCategoriesIds().size();

        BufferedImage tmpImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Color gray = new Color(184, 184, 184);

        Graphics2D tmpGraphic = tmpImage.createGraphics();
        Font font = new Font("TimesRoman", Font.BOLD, 16);
        tmpGraphic.setFont(font);
        tmpGraphic.setColor(Color.black);

        int maxAlternativeWidth = 0;
        FontMetrics metrics = tmpGraphic.getFontMetrics(font);
        for (int i = 0; i < inputs.getAlternativesIds().size(); i++) {
            String alternativeId = inputs.getAlternativesIds().get(i);
            if (maxAlternativeWidth < metrics.stringWidth(alternativeId)) {
                maxAlternativeWidth = metrics.stringWidth(alternativeId);
            }
        }

        int maxCategoryWidth = 0;
        for (int j = 0; j < inputs.getCategoriesIds().size(); j++) {
            String categoryId = inputs.getCategoriesIds().get(j);
            if (maxCategoryWidth < metrics.stringWidth(categoryId)) {
                maxCategoryWidth = metrics.stringWidth(categoryId);
            }
        }

        int categoryWidth = Math.max(60, maxCategoryWidth+10);
        int moveRight = 80 + maxAlternativeWidth;
        height = 100 * inputs.getAlternativesIds().size();
        width = categoryWidth * categoriesNumber + moveRight + 20;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setFont(font);

        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, width, height);

        for (int i = 0; i < inputs.getAlternativesIds().size(); i++) {
            g2d.setColor(Color.black);
            int y = ((100 - metrics.getHeight()) / 2) + metrics.getAscent() + i*100;
            g2d.drawString(inputs.getAlternativesIds().get(i), 20, y);


            String alternativeId = inputs.getAlternativesIds().get(i);
            String lowerId = inputs.getAssignments().get(alternativeId).get("LOWER");
            String upperId = inputs.getAssignments().get(alternativeId).get("UPPER");

            int lowerBound = inputs.getCategoriesRanking().get(lowerId);
            int upperBound = inputs.getCategoriesRanking().get(upperId);

            for (int j = lowerBound - 1; j < upperBound; j++) {
                g2d.setColor(gray);
                g2d.fillRect(moveRight+categoryWidth*j, 100*i+20, categoryWidth, 60);
            }

            for (int k = 0; k < inputs.getCategoriesIds().size(); k++) {
                String categoryId = inputs.getCategoriesIds().get(k);
                g2d.setColor(Color.black);
                int x = moveRight + categoryWidth*k + (categoryWidth - metrics.stringWidth(categoryId)) / 2;
                y = ((100 - metrics.getHeight()) / 2) + metrics.getAscent() + i*100;
                g2d.drawString(categoryId, x, y);
                g2d.drawLine(moveRight + k *categoryWidth, i*100 + 20, moveRight + k *categoryWidth, i*100 + 80);
            }
            g2d.drawLine(moveRight + categoriesNumber *categoryWidth, i*100 + 20, moveRight + categoriesNumber *categoryWidth, i*100 + 80);

            g2d.setColor(Color.black);
            g2d.drawLine(moveRight, i*100 + 20, moveRight + categoriesNumber*categoryWidth, i*100 + 20);
            g2d.drawLine(moveRight, i*100 + 80, moveRight + categoriesNumber*categoryWidth, i*100 + 80);
        }

        g2d.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static byte[] drawImageVersion2(InputsHandler.Inputs inputs) {
        byte[] bytes = null;

        int categoriesNumber = inputs.getCategoriesIds().size();
        int alternativesNumber = inputs.getAlternativesIds().size();

        BufferedImage tmpImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D tmpGraphic = tmpImage.createGraphics();
        Font font = new Font("TimesRoman", Font.BOLD, 16);
        tmpGraphic.setFont(font);
        tmpGraphic.setColor(Color.black);

        int maxAlternativeWidth = 0;
        FontMetrics metrics = tmpGraphic.getFontMetrics(font);
        for (int i = 0; i < inputs.getAlternativesIds().size(); i++) {
            String alternativeId = inputs.getAlternativesIds().get(i);
            if (maxAlternativeWidth < metrics.stringWidth(alternativeId)) {
                maxAlternativeWidth = metrics.stringWidth(alternativeId);
            }
        }

        int maxCategoryWidth = 0;
        for (int j = 0; j < inputs.getCategoriesIds().size(); j++) {
            String categoryId = inputs.getCategoriesIds().get(j);
            if (maxCategoryWidth < metrics.stringWidth(categoryId)) {
                maxCategoryWidth = metrics.stringWidth(categoryId);
            }
        }

        int categoryWidth = Math.max(60, maxCategoryWidth+10);
        int moveRight = 80 + maxAlternativeWidth;
        height = 70 * inputs.getAlternativesIds().size() + 60;
        width = categoryWidth * categoriesNumber + moveRight + 20;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setFont(font);

        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, width, height);

        Stroke normal = new BasicStroke(3);
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);



        for (int k = 0; k < inputs.getCategoriesIds().size(); k++) {
            String categoryId = inputs.getCategoriesIds().get(k);
            g2d.setColor(Color.black);
            int x = moveRight + categoryWidth*k + (categoryWidth - metrics.stringWidth(categoryId)) / 2;
            int y = ((40 - metrics.getHeight()) / 2) + metrics.getAscent();
            g2d.drawString(categoryId, x, y);

            g2d.setStroke(dashed);
            if (k != 0) {
                g2d.drawLine(moveRight + k * categoryWidth, 0, moveRight + k * categoryWidth, alternativesNumber * 100 + 60);
            }
        }

        for (int i = 0; i < inputs.getAlternativesIds().size(); i++) {
            g2d.setColor(Color.black);
            int y = ((70 - metrics.getHeight()) / 2) + metrics.getAscent() + i*70 + 40;
            g2d.drawString(inputs.getAlternativesIds().get(i), 20, y);


            String alternativeId = inputs.getAlternativesIds().get(i);
            String lowerId = inputs.getAssignments().get(alternativeId).get("LOWER");
            String upperId = inputs.getAssignments().get(alternativeId).get("UPPER");

            int lowerBound = inputs.getCategoriesRanking().get(lowerId);
            int upperBound = inputs.getCategoriesRanking().get(upperId);

            g2d.setStroke(normal);
            RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(moveRight + categoryWidth*(lowerBound-1), 70 * i + 50, categoryWidth * (upperBound - lowerBound + 1), 50, 10, 10);
            g2d.draw(roundedRectangle);
        }

        g2d.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
