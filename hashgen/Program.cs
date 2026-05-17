using System;
using BCrypt.Net;

class Program {
    static void Main() {
        var hash = BCrypt.Net.BCrypt.HashPassword("Test1234!");
        Console.WriteLine(hash);
    }
}
