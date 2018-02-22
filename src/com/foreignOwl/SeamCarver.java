package com.foreignOwl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;

/**
 * This class implements a SeamCarver object, that takes an input image and uses the seam carving
 * technique to change the dimensions of the image.
 */
public class SeamCarver {

    private BufferedImage input_image;
    private BufferedImage carved_image;
    private ArrayList<Seam> seams;

    /**
     * Default constructor for the Seam Carver.
     * @param input_path String indicating the path to the input image.
     * @throws IOException Throws IOException when file cannot be opened.
     */
    public SeamCarver(String input_path) throws IOException
    {
        this.carved_image = null;

        // Read input image
        try
        {
            File input_file = new File(input_path);
            this.input_image = ImageIO.read(input_file);
            carved_image = copyImage(input_image);
        }
        catch(IOException e)
        {
            System.out.println("Input file could not be opened: " + input_path);
            throw (e);
        }

        seams = new ArrayList<>();
    }

    /**
     * This method takes the input image and brings it down to the given width and height.
     * @param width Width of the output image in number of pixels.
     * @param height Height of the output image in number of pixels.
     * @throws IllegalArgumentException Throws the exception when width or height is larger than the that of the input image, or equal/smaller than 0.
     */
    public void carveImage(int width, int height) throws IllegalArgumentException
    {
        // get the number of horizontal and vertical seams that we need to remove
        int num_carve_horizontal = input_image.getHeight() - height;
        int num_carve_vertical = input_image.getWidth() - width;

        int total_carve = num_carve_horizontal + num_carve_vertical;

        // if the dimensions of the output image is larger than the input, throw an exception
        if(num_carve_horizontal < 0 || num_carve_horizontal >= input_image.getWidth() || num_carve_vertical < 0 || num_carve_vertical >= input_image.getHeight())
        {
            System.out.println("Width and height of the output image should be less then or equal to the original!");
            throw new IllegalArgumentException();
        }


        // Remove seams until we reach the output dimensions
        while(num_carve_horizontal > 0 || num_carve_vertical > 0)
        {
            Seam horizontal_seam;
            Seam vertical_seam;

            getProgress(total_carve,num_carve_horizontal+num_carve_vertical);

            // get the current energy table
            double[][] energy_table = calculateEnergyTable(carved_image);

            // if we can remove either horizontal or vertical seam
            // compare their energy values, remove the one with the minimum
            if(num_carve_horizontal > 0 && num_carve_vertical > 0)
            {
                horizontal_seam = getHorizontalSeam(energy_table);
                vertical_seam = getVerticalSeam(energy_table);

                if(horizontal_seam.getEnergy() < vertical_seam.getEnergy())
                {
                    seams.add(horizontal_seam);
                    removeSeam(horizontal_seam);
                    num_carve_horizontal--;
                }
                else
                {
                    seams.add(vertical_seam);
                    removeSeam(vertical_seam);
                    num_carve_vertical--;
                }
            }

            // if we should remove horizontal seam, remove it
            else if(num_carve_horizontal > 0)
            {
                horizontal_seam = getHorizontalSeam(energy_table);
                seams.add(horizontal_seam);
                removeSeam(horizontal_seam);
                num_carve_horizontal--;
            }

            // if we should remove vertical seam, remove it
            else
            {
                vertical_seam = getVerticalSeam(energy_table);
                seams.add(vertical_seam);
                removeSeam(vertical_seam);
                num_carve_vertical--;
            }

        }

    }

    /**
     * This method takes and image and calculates energy of the each pixel using dual energy
     * gradient.
     * @param image The image whose energy table will be calculated.
     * @return Double type 2d array with same dimensions as the input image.
     */
    private double[][] calculateEnergyTable(BufferedImage image)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] energyTable = new double[width][height];

        // loop over each pixel
        for(int i = 0; i < width; i++)
        {
            for(int j = 0; j < height; j++)
            {
                double xEnergy, yEnergy, totalEnergy;

                // get next and previous horizontal pixels
                int xPrevRGB = image.getRGB((i-1+width)%width, j);
                int xNextRGB = image.getRGB((i+1+width)%width, j);

                // calculate the horizontal energy
                xEnergy = getEnergy(xPrevRGB, xNextRGB);

                // get next and previous vertical pixels
                int yPrevRGB = image.getRGB(i,(j-1+height)%height);
                int yNextRGB = image.getRGB(i, (j+1+height)%height);

                // calculate the vertical energy
                yEnergy = getEnergy(yPrevRGB, yNextRGB);

                // get the total energy
                totalEnergy = xEnergy + yEnergy;
                energyTable[i][j] = totalEnergy;
            }
        }

        return energyTable;
    }


    /**
     * Given two rgb integers this method calculates the dual energy gradient.
     * @param rgb1 An integer rgb value.
     * @param rgb2 An integer rgb value.
     * @return The dual energy gradient of the given two rgb integers.
     */
    private double getEnergy(int rgb1, int rgb2)
    {
        // get r, g, b values of rgb1
        double b1 = (rgb1) & 0xff;
        double g1 = (rgb1 >> 8) & 0xff;
        double r1 = (rgb1 >> 16) & 0xff;

        // get r, g, b values of rgb2
        double b2 = (rgb2) & 0xff;
        double g2 = (rgb2 >> 8) & 0xff;
        double r2 = (rgb2 >> 16) & 0xff;

        double energy = (r1-r2)*(r1-r2) + (g1-g2)*(g1-g2) + (b1-b2)*(b1-b2);

        return energy;


    }

    /**
     * This method finds a horizontal seam with minimum energy in the given energy table.
     * @param energyTable The energy table that will be used to find a horizontal seam.
     * @return A horizontal seam with minimum energy.
     */
    private Seam getHorizontalSeam(double[][] energyTable)
    {
        int width = energyTable.length;
        int height = energyTable[0].length;

        // initialize the seam that will be returned
        Seam seam = new Seam(width, "horizontal");

        // 2d array keeps the dynamic solution
        double[][] horizontal_dp = new double[width][height];

        // 2d array for backtracking
        int[][] prev = new int[width][height];

        // loop over all the pixels
        for(int i = 0; i < width; i++)
        {
            for(int j = 0; j < height; j++)
            {
                double min_value;

                // base case
                if(i == 0)
                {
                    horizontal_dp[i][j] = energyTable[i][j];
                    prev[i][j] = -1;
                    continue;
                }

                // if on the edge, there are 2 pixels to take the minimum
                else if(j == 0 )
                {
                    min_value = Math.min(horizontal_dp[i-1][j], horizontal_dp[i-1][j+1]);
                    if(min_value == horizontal_dp[i-1][j])
                    {
                        prev[i][j] = j;
                    }
                    else
                    {
                        prev[i][j] = j+1;
                    }
                }
                // if on the edge, there are 2 pixels to take the minimum
                else if(j == height - 1)
                {
                    min_value = Math.min(horizontal_dp[i-1][j], horizontal_dp[i-1][j-1]);
                    if(min_value == horizontal_dp[i-1][j])
                    {
                        prev[i][j] = j;
                    }
                    else
                    {
                        prev[i][j] = j-1;
                    }
                }
                // otherwise take the minimum of three neighbor pixels
                else
                {
                    min_value = Math.min(horizontal_dp[i-1][j], Math.min(horizontal_dp[i-1][j-1],horizontal_dp[i-1][j+1]));

                    if(min_value == horizontal_dp[i-1][j])
                    {
                        prev[i][j] = j;
                    }
                    else if (min_value == horizontal_dp[i-1][j-1])
                    {
                        prev[i][j] = j-1;
                    }
                    else
                    {
                        prev[i][j] = j+1;
                    }


                }

                // add min value to the current energy
                horizontal_dp[i][j] = energyTable[i][j] + min_value;
            }
        }


        // find the minimum total energy on the edge
        // and its coordinate
        double min_energy = horizontal_dp[width-1][0];
        int min_coord = 0;
        for(int j = 0; j < height; j++)
        {
            if(min_energy > horizontal_dp[width-1][j])
            {
                min_energy = horizontal_dp[width-1][j];
                min_coord = j;
            }
        }

        seam.setEnergy(min_energy);

        // backtrack from the minimum, and build the seam
        for(int i = width-1; i >= 0; i--)
        {
            seam.setPixels(i, min_coord);
            min_coord = prev[i][min_coord];
        }

        return seam;
    }

    /**
     * This method finds a vertical seam with minimum energy in the given energy table.
     * @param energyTable The energy table that will be used to find a vertical seam.
     * @return A vertical seam with minimum energy.
     */
    private Seam getVerticalSeam(double[][] energyTable)
    {
        int width = energyTable.length;
        int height = energyTable[0].length;

        // initialize the seam that will be returned
        Seam seam = new Seam(height, "vertical");

        // 2d array keeps the dynamic solution
        double[][] vertical_dp = new double[width][height];

        // 2d array for backtracking
        int[][] prev = new int[width][height];

        // loop over all the pixels
        for(int j = 0; j < height; j++)
        {
            for(int i = 0; i < width; i++)
            {
                double min_value;

                // base case
                if(j == 0)
                {
                    vertical_dp[i][j] = energyTable[i][j];
                    prev[i][j] = -1;
                    continue;
                }
                // if on the edge, there are 2 pixels to take the minimum
                else if(i == 0 )
                {
                    min_value = Math.min(vertical_dp[i][j-1], vertical_dp[i+1][j-1]);
                    if(min_value == vertical_dp[i][j-1])
                    {
                        prev[i][j] = i;
                    }
                    else
                    {
                        prev[i][j] = i+1;
                    }
                }
                // if on the edge, there are 2 pixels to take the minimum
                else if(i == width - 1)
                {
                    min_value = Math.min(vertical_dp[i][j-1], vertical_dp[i-1][j-1]);
                    if(min_value == vertical_dp[i][j-1])
                    {
                        prev[i][j] = i;
                    }
                    else
                    {
                        prev[i][j] = i-1;
                    }
                }
                // otherwise take the minimum of three neighbor pixels
                else
                {
                    min_value = Math.min(vertical_dp[i][j-1], Math.min(vertical_dp[i-1][j-1],vertical_dp[i+1][j-1]));

                    if(min_value == vertical_dp[i][j-1])
                    {
                        prev[i][j] = i;
                    }
                    else if (min_value == vertical_dp[i-1][j-1])
                    {
                        prev[i][j] = i-1;
                    }
                    else
                    {
                        prev[i][j] = i+1;
                    }


                }

                // add min value to the current energy
                vertical_dp[i][j] = energyTable[i][j] + min_value;
            }
        }


        // find the minimum total energy on the edge
        // and its coordinate
        double min_energy = vertical_dp[0][height-1];
        int min_coord = 0;
        for(int i = 0; i < width; i++)
        {
            if(min_energy > vertical_dp[i][height-1])
            {
                min_energy = vertical_dp[i][height-1];
                min_coord = i;
            }
        }

        seam.setEnergy(min_energy);

        // backtrack from the minimum, and build the seam
        for(int j = height-1; j >= 0; j--)
        {
            seam.setPixels(j, min_coord);
            min_coord = prev[min_coord][j];
        }

        return seam;
    }

    /**
     * This method removes a given seam from the image.
     * @param seam The seam that will be removed.
     */
    private void removeSeam(Seam seam)
    {
        int width = carved_image.getWidth();
        int height = carved_image.getHeight();
        BufferedImage image_new;


        if(seam.getDirection().equals("horizontal")) {
            // decrement height by 1
            image_new = new BufferedImage(width, height - 1, BufferedImage.TYPE_INT_RGB);

            // loop over all pixels
            for (int i = 0; i < width; i++)
            {
                boolean moveToNext = false;
                for (int j = 0; j < height - 1; j++) {
                    // once we run into the pixel in the seam
                    // skip it and keep copying from the next one
                    if (seam.getPixels()[i] == j) {
                        moveToNext = true;
                    }
                    if(moveToNext)
                        image_new.setRGB(i, j, carved_image.getRGB(i, j+1));
                    else
                        image_new.setRGB(i, j, carved_image.getRGB(i, j));
                }
            }
        }
        else
        {
            // decrement the width by 1
            image_new = new BufferedImage(width-1, height, BufferedImage.TYPE_INT_RGB);

            // loop over all pixels
            for(int j = 0; j < height; j++)
            {
                boolean moveToNext = false;
                for(int i = 0; i < width-1; i++)
                {
                    // once we run into the pixel in the seam
                    // skip it and keep copying from the next one
                    if(seam.getPixels()[j] == i) {
                        moveToNext = true;
                    }
                    if(moveToNext) {
                        image_new.setRGB(i, j, carved_image.getRGB(i + 1, j));
                    }
                    else {
                        image_new.setRGB(i, j, carved_image.getRGB(i, j));
                    }
                }
            }
        }

        // update the carved image
        carved_image = image_new;
    }

    /**
     * This method saves the carved image to the given file path with the given extension type.
     * @param file_path File path to the output carved image.
     * @param type Extension type for the output file.
     * @throws IOException Throws IOException when file cannot be created.
     */
    public void saveCarvedImage(String file_path, String type) throws IOException
    {
        try {
            File output_file = new File(file_path);
            ImageIO.write(carved_image, type, output_file);
            System.out.println("Writing completed.");
        } catch (IOException e)
        {
            System.out.println("Cannot open output file: " + file_path);
            throw (e);
        }
    }

    /**
     * This method saves the energy table of the current image.
     * @throws IOException Throws IOException when file cannot be created.
     */
    public void saveEnergyTable() throws IOException
    {
        saveEnergyTable(input_image);
    }

    /**
     * This method saves the energy table of a given image.
     * @param image The image whose energy table will be saved.
     * @throws IOException Throws IOException when file cannot be created.
     */
    public void saveEnergyTable(BufferedImage image) throws IOException
    {
        // calculate the energy table
        double[][] energyTable = calculateEnergyTable(image);

        // create the energy image
        BufferedImage energy_image = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);


        //find the max value in the energy table
        double maxValue = energyTable[0][0];
        for(int i = 0; i < energyTable.length; i++)
            for(int j = 0; j < energyTable[0].length;j++)
                maxValue = Math.max(maxValue, energyTable[i][j]);

        // loop over each pixel
        for(int i = 0; i < energy_image.getWidth(); i++)
        {
            for(int j = 0; j < energy_image.getHeight(); j++)
            {
                // calculate the rgb value (scaled for grayscale)
                int gray = (int) ((energyTable[i][j]/maxValue)*256);
                int rgb = (gray << 16) + (gray << 8) + gray;
                energy_image.setRGB(i,j,rgb);
            }
        }


        //save the energy table image
        try {
            File output_file = new File("energyTable.jpg");
            ImageIO.write(energy_image, "jpg", output_file);
            System.out.println("Energy table has been created.");
        } catch (IOException e)
        {
            System.out.println("Cannot create file for energy table.");
            throw (e);
        }
    }


    /**
     * This method saves the seam table, which is the input image with all removed seams marked.
     * @throws IOException Throws IOException when file cannot be created.
     */
    public void saveSeamTable() throws IOException
    {
        System.out.println("Creating seam table...");

        // 2d array will keep the rgb values for the seam table image
        int[][] seam_image_array = new int[input_image.getWidth()][input_image.getHeight()];


        int current_width = carved_image.getWidth();
        int current_height = carved_image.getHeight();

        // copy the carved image to the array
        for(int i = 0; i < current_width; i++)
            for(int j = 0; j < current_height; j++)
                seam_image_array[i][j] = carved_image.getRGB(i,j);


        // loop over the seams
        for(int s = seams.size()-1; s >= 0; s--)
        {
            getProgress(seams.size(),s+1);

            // for each seam
            Seam seam = seams.get(s);
            if(seam.getDirection().equals("horizontal"))
            {
                for(int i = 0; i < current_width; i++)
                {
                    int coord = seam.getPixels()[i];
                    for(int j = current_height; j > coord; j--)
                        seam_image_array[i][j] = seam_image_array[i][j-1];
                    seam_image_array[i][coord] = (0xff << 16);

                }
                current_height++;
            }
            else
            {
                for(int j = 0; j < current_height; j++)
                {
                    //System.out.println(j + " " + current_height);
                    int coord = seam.getPixels()[j];
                    for(int i = current_width; i > coord; i--)
                        seam_image_array[i][j] = seam_image_array[i-1][j];
                    seam_image_array[coord][j] = (0xff << 16);
                }

                current_width++;
            }

        }


        // create the seam image
        BufferedImage seam_image = new BufferedImage(input_image.getWidth(), input_image.getHeight(), BufferedImage.TYPE_INT_RGB);

        // set rgb of the image using the array
        for(int i = 0; i < seam_image.getWidth(); i++)
        {
            for(int j = 0; j < seam_image.getHeight(); j++)
            {
                seam_image.setRGB(i,j,seam_image_array[i][j]);
            }
        }


        // save the seam table
        try {
            File output_file = new File("seamTable.jpg");
            ImageIO.write(seam_image, "jpg", output_file);
            System.out.println("Seam table has been created.");
        } catch (IOException e)
        {
            System.out.println("Cannot create file for seam table.");
            throw (e);
        }
    }

    /**
     * Given a BufferedImage instance it copies it and returns as a different BufferedImage instance.
     * @param img The image that will be copied.
     * @return The new instance, copy of the given image.
     */
    private BufferedImage copyImage(BufferedImage img)
    {
        ColorModel color_model = img.getColorModel();
        boolean isAlphaPremultiplied = color_model.isAlphaPremultiplied();
        WritableRaster writable_raster = img.copyData(null);

        return new BufferedImage(color_model, writable_raster, isAlphaPremultiplied, null);

    }

    /**
     * This method prints the progress (remaining operations/total operations) to the console/
     * @param t Total number of operations.
     * @param c Completed operations.
     */
    private void getProgress(int t, int c)
    {
        double total = t;
        double current = c;

        String message = String.format("%.2f", (total-current)/total*100);
        System.out.println( message + "%");
    }
}
