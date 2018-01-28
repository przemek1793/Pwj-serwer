import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Properties;

public class Serwer implements Runnable
{
    Socket csocket;
    Serwer(Socket csocket)
    {
        this.csocket = csocket;
    }

    static boolean ladujSterownik()
    {
        // LADOWANIE STEROWNIKA
        System.out.print("Sprawdzanie sterownika:");
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            return true;
        } catch (Exception e) {
            System.out.println("Blad przy ladowaniu sterownika bazy!");
            return false;
        }
    }

    /**
     * Metoda s�u�y do po��czenia z MySQL bez wybierania konkretnej bazy
     *
     * @return referencja do uchwytu bazy danych
     */
    public static Connection getConnection(String adres, int port) {

        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", "PWJ");
        connectionProps.put("password", "asdf");

        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + adres + ":" + port + "/",
                    connectionProps);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * Metoda s�u�y do nawi�zania po��czenia z baz� danych
     *
     * @param adress
     *            - adres bazy danych
     * @param dataBaseName
     *            - nazwa bazy
     * @param userName
     *            - login do bazy
     * @param password
     *            - has�o do bazy
     * @return - po��czenie z baz�
     */
    private static Connection connectToDatabase(String adress,
                                                String dataBaseName, String userName, String password)
    {
        String baza = "jdbc:mysql://" + adress + "/" + dataBaseName;
        // objasnienie opisu bazy:
        // jdbc: - mechanizm laczenia z baza (moze byc inny, np. odbc)
        // mysql: - rodzaj bazy
        // adress - adres serwera z baza (moze byc tez w nazwy)
        // dataBaseName - nazwa bazy
        java.sql.Connection connection = null;
        try {
            connection = DriverManager.getConnection(baza, userName, password);
            System.out.println("Polaczono z baza");
        } catch (SQLException e) {
            System.out.println("Nie polaczono z baza!");
            /**
             * utworzenie bazy w przypadku gdy jej nie wykryto
             */
            try
            {
                Connection con=getConnection(adress,3306);
                Statement st = createStatement(con);
                if (executeUpdate(st, "USE "+dataBaseName+";") != -1)
                    System.out.println("Baza wybrana");
                else
                {
                    if (executeUpdate(st, "create Database "+dataBaseName+";") != -1)
                        System.out.println("Baza utworzona");
                    else
                        System.out.println("Baza niewybrana!");
                    connection=connectToDatabase(adress,dataBaseName,userName,password);
                }
            }
            catch (Exception e1)
            {
                connection=connectToDatabase(adress,dataBaseName,userName,password);
            }

 //           if (executeUpdate(st, "CREATE TABLE Administrator (id INT unique primary key NOT NULL, login VARCHAR(50) NOT NULL, haslo VARCHAR(50) NOT NULL, Imie VARCHAR(50) NOT NULL, Nazwisko VARCHAR(50) NOT NULL, Mail VARCHAR(50) NOT NULL, adres VARCHAR(50), telefon VARCHAR(50) NOT NULL );") != -1)
 //               System.out.println("Tabela Administrator utworzona");
 //           else
 //               System.out.println("Tabela Administrator nie utworzona!");

        }
        Statement st = createStatement(connection);
        //Sprawdzenie czy tabele istnieja, a jesli nie to utworzenie ich
        try
        {
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "Uzytkownicy", null);
            if (tables.next()) {
                System.out.println("Tabela Uzytkownicy istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Uzytkownicy (login VARCHAR(50) unique NOT NULL, haslo VARCHAR(50) NOT NULL, Imie VARCHAR(50) NOT NULL, Nazwisko VARCHAR(50) primary key NOT NULL, Email VARCHAR(50) NOT NULL, typ enum(\"student\",\"prowadzacy\",\"administrator\") NOT NULL, Prowadzone_przedmioty VARCHAR(250), Uczeszczane_przedmioty VARCHAR(250) );") != -1)
                    System.out.println("Tabela Uzytkownicy utworzona");
                else
                    System.out.println("Tabela Uzytkownicy nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Przedmioty", null);
            if (tables.next()) {
                System.out.println("Tabela Przedmioty istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Przedmioty (Nazwa VARCHAR(50) unique NOT NULL, Nazwisko_prowadzacego VARCHAR(50) primary key NOT NULL, Preferowany_czas_prowadzacego VARCHAR(50), Godziny_przedmiotu VARCHAR(50), Uczeszczajacy VARCHAR(250) );") != -1)
                    System.out.println("Tabela Przedmioty utworzona");
                else
                    System.out.println("Tabela Przedmioty nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Serwery", null);
            if (tables.next()) {
                System.out.println("Tabela Serwery istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Serwery (IP VARCHAR(50) unique NOT NULL);") != -1)
                    System.out.println("Tabela Serwery utworzona");
                else
                    System.out.println("Tabela Serwery nie utworzona!");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * tworzenie obiektu Statement przesy�aj�cego zapytania do bazy connection
     *
     * @param connection
     *            - po��czenie z baz�
     * @return obiekt Statement przesy�aj�cy zapytania do bazy
     */
    private static Statement createStatement(Connection connection) {
        try {
            return connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ;
        return null;
    }

    /**
     * Wykonanie kwerendy i przes�anie wynik�w do obiektu ResultSet
     *
     * @param s
     *            - Statement
     * @param sql
     *            - zapytanie
     * @return wynik
     */
    private static ResultSet executeQuery(Statement s, String sql) {
        try {
            return s.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Wy�wietla dane uzyskane zapytaniem select
     *
     * @param r
     *            - wynik zapytania
     */
    private static void printDataFromQuery(ResultSet r) {
        ResultSetMetaData rsmd;
        try {
            rsmd = r.getMetaData();
            int numcols = rsmd.getColumnCount(); // pobieranie liczby column
            // wyswietlanie nazw kolumn:
            for (int i = 1; i <= numcols; i++) {
                System.out.print("\t" + rsmd.getColumnLabel(i) + "\t|");
            }
            System.out.print("\n____________________________________________________________________________\n");
            /**
             * r.next() - przej�cie do kolejnego rekordu (wiersza) otrzymanych
             * wynik�w
             */
            // wyswietlanie kolejnych rekordow:
            while (r.next()) {
                for (int i = 1; i <= numcols; i++) {
                    Object obj = r.getObject(i);
                    if (obj != null)
                        System.out.print("\t" + obj.toString() + "\t|");
                    else
                        System.out.print("\t");
                }
                System.out.println();
            }
        } catch (SQLException e) {
            System.out.println("Bl�d odczytu z bazy! " + e.toString());
            System.exit(3);
        }
    }

    /**
     * Zamykanie po��czenia z baz� danych
     *
     * @param connection
     *            - po��czenie z baz�
     * @param s
     *            - obiekt przesy�aj�cy zapytanie do bazy
     */
    private static void closeConnection(Connection connection, Statement s) {
        System.out.print("\nZamykanie polaczenia z baza�:");
        try {
            s.close();
            connection.close();
        } catch (SQLException e) {
            System.out
                    .println("Bl�d przy zamykaniu pol�czenia " + e.toString());
            System.exit(4);
        }
        System.out.print(" zamkni�cie OK");
    }

    private static int executeUpdate(Statement s, String sql) {
        try {
            return s.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void run()
    {
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(csocket.getOutputStream()));
            Menu(in,out);
        }
        catch (IOException ex)
        {
            System.out.println("IOException");
        }
    }

    public void Menu (BufferedReader in, PrintWriter out)
    {
        try
        {
            /**
             * na podstawie pierwszej wyslanej lini serwer decyduje co ma zrobic
             */
            String tekst=in.readLine();
            /**
             * rejestracja
             */
    //        if(tekst.equals("rejestracja"))
  //          {
  //              rejestracja(in,out);
 //           }
            /**
             * logowanie
             */
//            if(tekst.equals("logowanie"))
 //           {
 //               logowanie(in,out);
 //           }
        }
        catch (IOException ex)
        {
            System.out.println("IOException w menu");
        }
    }

    public static void main(String args[]) throws Exception
    {
        if (ladujSterownik())
            System.out.println(" sterownik OK");
        else
            System.exit(1);
        connectToDatabase("192.168.0.13","PWJ_Projekt","PWJ","asdf");
        ServerSocket ssock = new ServerSocket(4255);
        while (true)
        {
            Socket sock = ssock.accept();
            new Thread(new Serwer(sock)).start();
        }
    }
}
