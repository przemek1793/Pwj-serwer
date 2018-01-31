import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Properties;

public class Serwer implements Runnable
{
    Socket csocket;
    static String AdresBazyDanych="192.168.0.13";
    static String NazwaBazyDanych="PWJ_Projekt";
    static String NazwaUzytkownika="PWJ";
    static String HasłoDoBazy="asdf";
    String TypZalogowanego="";
    String ObecnieZalogowany="";
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
    private static Connection getConnection(String adres, int port) {

        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", NazwaUzytkownika);
        connectionProps.put("password", HasłoDoBazy);

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
                if (executeUpdate(st, "USE "+dataBaseName+";") > -1)
                    System.out.println("Baza wybrana");
                else
                {
                    if (executeUpdate(st, "create Database "+dataBaseName+";") > -1)
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
                if (executeUpdate(st, "CREATE TABLE Uzytkownicy (login VARCHAR(50) unique NOT NULL, haslo VARCHAR(50) NOT NULL, Imie VARCHAR(50) NOT NULL, Nazwisko VARCHAR(50) NOT NULL, Email VARCHAR(50) unique NOT NULL, typ enum(\"student\",\"prowadzacy\",\"administrator\") NOT NULL, Prowadzone_przedmioty VARCHAR(250), Uczeszczane_przedmioty VARCHAR(250), CzyZatwierdzony tinyint(1) not null);") > -1)
                    System.out.println("Tabela Uzytkownicy utworzona");
                else
                    System.out.println("Tabela Uzytkownicy nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Przedmioty", null);
            if (tables.next()) {
                System.out.println("Tabela Przedmioty istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Przedmioty (Nazwa VARCHAR(50) unique NOT NULL, Nazwisko_prowadzacego VARCHAR(50) NOT NULL, Preferowany_czas_prowadzacego VARCHAR(50), Godziny_przedmiotu VARCHAR(50), Uczeszczajacy VARCHAR(250) );") > -1)
                    System.out.println("Tabela Przedmioty utworzona");
                else
                    System.out.println("Tabela Przedmioty nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Serwery", null);
            if (tables.next()) {
                System.out.println("Tabela Serwery istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Serwery (IP VARCHAR(50) unique NOT NULL);") > -1)
                    System.out.println("Tabela Serwery utworzona");
                else
                    System.out.println("Tabela Serwery nie utworzona!");
            }
            tables = dbm.getTables(null, null, "Zmiany", null);
            if (tables.next()) {
                System.out.println("Tabela Zmiany istnieje");
            }
            else {
                if (executeUpdate(st, "CREATE TABLE Zmiany (Tabela VARCHAR(50) NOT NULL, Klucz VARCHAR(50) NOT NULL, KolumnaDoZmiany VARCHAR(50) NOT NULL, NowaWartosc VARCHAR(50) NOT NULL);") > -1)
                    System.out.println("Tabela Zmiany utworzona");
                else
                    System.out.println("Tabela Zmiany nie utworzona!");
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
        }
        catch (MySQLIntegrityConstraintViolationException sqle)
        {
            return -5; //5 - dodawanie loginu, lub email który już jest w tabeli
        }
        catch (SQLException e) {
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

    private void Menu (BufferedReader in, PrintWriter out)
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
            if(tekst.equals("rejestracja"))
            {
                rejestracja(in,out);
            }
            /**
             * logowanie
             */
            if(tekst.equals("logowanie"))
            {
                logowanie(in,out);
            }
            if(tekst.equals("zmiana_danych"))
            {
                System.out.println("ładowanie danych");
                wyslijDaneDoZmiany(in, out);
            }
            if(tekst.equals("przypomnienie"))
            {
                System.out.println("przypomnienie hasła");
                przypomnienie(in,out);
            }
            if(tekst.equals("zmiana"))
            {
                System.out.println("zmiana danych");
                zmianaDanych(in,out);
            }
            if(tekst.equals("lista_prowadzacych"))
            {
                System.out.println("Wysyłanie listy wykładowców");
                wyslijListeWykladowcow(in,out);
            }
            if(tekst.equals("nowy_przedmiot"))
            {
                System.out.println("Tworzenie nowego przedmiotu");
                nowyPrzedmiot(in,out);
            }
            if(tekst.equals("wylogowanie"))
            {
                System.out.println("wylogowano");
                TypZalogowanego="";
                ObecnieZalogowany="";
                Menu(in,out);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception
    {
        if (ladujSterownik())
            System.out.println(" sterownik OK");
        else
            System.exit(1);
        ServerSocket ssock = new ServerSocket(4255);
        while (true)
        {
            Socket sock = ssock.accept();
            new Thread(new Serwer(sock)).start();
        }
    }

    private void logowanie(BufferedReader in, PrintWriter out)
    {
        String login,hasło;
        try
        {
            login = in.readLine();
            hasło = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE haslo='"+hasło+"' and login='"+login+"';");
            if (wyniklogowania.next())
            {
                String typ= wyniklogowania.getString("typ");
                System.out.println("Udalo sie zalogowac");
                out.println("poprawne");
                out.println(typ);
                TypZalogowanego=typ;
                ObecnieZalogowany=login;
            }
            else
            {
                System.out.println("bledne dane");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void rejestracja(BufferedReader in, PrintWriter out)
    {
        String login,hasło,Imie,Nazwisko,Email,typ;
        try
        {
            login = in.readLine();
            hasło = in.readLine();
            Imie = in.readLine();
            Nazwisko = in.readLine();
            Email = in.readLine();
            typ = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            if (executeUpdate(st, "INSERT INTO uzytkownicy (login, haslo, Imie, Nazwisko, Email, typ, CzyZatwierdzony) values ('"+login+"', '"+hasło+"', '"+Imie+"', '"+Nazwisko+"', '"+Email+"', '"+typ+"', 0);") > -1)
            {
                System.out.println("Zarejestrowano uzytkownika");
                out.println("ok");
            }
            else if (executeUpdate(st, "INSERT INTO uzytkownicy (login, haslo, Imie, Nazwisko, Email, typ, CzyZatwierdzony) values ('"+login+"', '"+hasło+"', '"+Imie+"', '"+Nazwisko+"', '"+Email+"', '"+typ+"', 0);") == -5)
            {
                System.out.println("Duplikat loginu lub email");
                out.println("duplikat");
            }
            else
            {
                System.out.println("Nie zarejestrowano uzytkownika!");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

    }

    private void przypomnienie(BufferedReader in, PrintWriter out)
    {
        String email;
        try
        {
            email = in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE Email='"+email+"';");
            if (wyniklogowania.next())
            {
                System.out.println("Znaleziono adres email w bazie");
                String login= wyniklogowania.getString("login");
                String haslo= wyniklogowania.getString("haslo");
                try
                {
                    GoogleMail.send(GoogleMail.getGmailService(),email,"","pwj.planlekcji@gmail.com","Pwj - plan lekcji. Przypomnienie hasła","login - "+login+"\nhasło - "+haslo );
                    out.println("poprawne");
                }
                catch (GoogleJsonResponseException e)
                {
                    out.println("zły email");
                }
            }
            else
            {
                System.out.println("bledne dane");
                out.println("bledne");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void wyslijDaneDoZmiany(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM uzytkownicy WHERE login='"+ObecnieZalogowany+"';");
            if (wyniklogowania.next())
            {
                String haslo,imie,nazwisko,email;
                haslo= wyniklogowania.getString("haslo");
                imie= wyniklogowania.getString("Imie");
                nazwisko= wyniklogowania.getString("Nazwisko");
                email= wyniklogowania.getString("Email");
                out.println(ObecnieZalogowany);
                out.println(haslo);
                out.println(imie);
                out.println(nazwisko);
                out.println(email);
            }
            else
            {
                System.out.println("Brak zalogowanego uzytkownika w bazie");
                out.println("brak");
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void zmianaDanych(BufferedReader in, PrintWriter out)
    {
        try
        {
            String login,hasło,imie,nazwisko,email;
            login=in.readLine();
            hasło=in.readLine();
            imie=in.readLine();
            nazwisko=in.readLine();
            email=in.readLine();
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            if (executeUpdate(st, "UPDATE uzytkownicy SET login='"+login+"', haslo='"+hasło+"', Imie='"+imie+"', Nazwisko='"+nazwisko+"', Email='"+email+"' WHERE login='"+ObecnieZalogowany+"';") > -1)
            {
                System.out.println("Zmieniono dane");
                ObecnieZalogowany=login;
                out.println("ok");
            }
            else if (executeUpdate(st, "UPDATE uzytkownicy SET login='"+login+"', haslo='"+hasło+"', Imie='"+imie+"', Nazwisko='"+nazwisko+"', Email='"+email+"' WHERE login='"+ObecnieZalogowany+"';") == -5)
            {
                System.out.println("Duplikat loginu lub email");
                out.println("duplikat");
            }
            else
            {
                System.out.println("Nie zmieniono danych!");
                out.println("bledne");
            }
            out.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Menu(in,out);
    }

    private void wyslijListeWykladowcow(BufferedReader in, PrintWriter out)
    {
        try
        {
            Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
            Statement st = createStatement(con);
            if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                System.out.println("Baza wybrana");
            else
                System.out.println("Baza niewybrana!");
            ResultSet wyniklogowania = executeQuery(st, "SELECT * FROM `uzytkownicy` WHERE typ='prowadzacy';");
            int size= 0;
            if (wyniklogowania != null)
            {
                wyniklogowania.beforeFirst();
                wyniklogowania.last();
                size = wyniklogowania.getRow();
                wyniklogowania.beforeFirst();
            }
            out.println(size);
            while (wyniklogowania.next())
            {
                String nazwisko;
                nazwisko= wyniklogowania.getString("Nazwisko");
                out.println(nazwisko);
            }
            out.flush();
            Menu(in,out);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void nowyPrzedmiot(BufferedReader in, PrintWriter out)
    {
        String nazwa,Nazwisko;
        try
        {
            if (!TypZalogowanego.equals("administrator"))
            {
                out.println("brak uprawnien");
            }
            else
            {
                out.println("ok");
                out.flush();
                nazwa = in.readLine();
                Nazwisko = in.readLine();
                Connection con = connectToDatabase(AdresBazyDanych,NazwaBazyDanych,NazwaUzytkownika,HasłoDoBazy);
                Statement st = createStatement(con);
                if (executeUpdate(st, "USE "+NazwaBazyDanych+";") > -1)
                    System.out.println("Baza wybrana");
                else
                    System.out.println("Baza niewybrana!");
                if (executeUpdate(st, "Insert into `przedmioty` (Nazwa, Nazwisko_prowadzacego) values ('"+nazwa+"', '"+Nazwisko+"')") > -1)
                {
                    System.out.println("Dodano przedmiot");
                    out.println("ok");
                }
                else if (executeUpdate(st, "Insert into `przedmioty` (Nazwa, Nazwisko_prowadzacego) values ('"+nazwa+"', '"+Nazwisko+"')") == -5)
                {
                    System.out.println("Duplikat nazwy");
                    out.println("duplikat");
                }
                else
                {
                    System.out.println("Nie dodano przedmiotu!");
                    out.println("bledne");
                }
            }
            out.flush();
            Menu(in,out);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

    }
}
