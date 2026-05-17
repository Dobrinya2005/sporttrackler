using FitnessTrainerAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Data;

public static class DbSeeder
{
    public static async Task SeedAsync(AppDbContext db)
    {
        await SeedUsersAsync(db);
        await SeedFoodAsync(db);
    }

    private static async Task SeedUsersAsync(AppDbContext db)
    {
        if (await db.Users.AnyAsync()) return;

        var adminRole   = await db.Roles.FirstAsync(r => r.RoleName == "Admin");
        var trainerRole = await db.Roles.FirstAsync(r => r.RoleName == "Trainer");
        var clientRole  = await db.Roles.FirstAsync(r => r.RoleName == "Client");

        // ── Администратор ────────────────────────────────────────
        var admin = new User
        {
            RoleId       = adminRole.RoleId,
            FirstName    = "Администратор",
            LastName     = "SportTrackler",
            Email        = "admin@sporttrackler.ru",
            PasswordHash = BCrypt.Net.BCrypt.HashPassword("Admin123!"),
            Phone        = "+7 900 000-00-01",
            IsActive     = true
        };
        db.Users.Add(admin);

        // ── Тренер ───────────────────────────────────────────────
        var trainer = new User
        {
            RoleId       = trainerRole.RoleId,
            FirstName    = "Алексей",
            LastName     = "Иванов",
            Email        = "trainer@sporttrackler.ru",
            PasswordHash = BCrypt.Net.BCrypt.HashPassword("Trainer123!"),
            Phone        = "+7 900 000-00-02",
            IsActive     = true
        };
        db.Users.Add(trainer);

        // ── Тестовый клиент ──────────────────────────────────────
        var client = new User
        {
            RoleId       = clientRole.RoleId,
            FirstName    = "Мария",
            LastName     = "Петрова",
            Email        = "client@sporttrackler.ru",
            PasswordHash = BCrypt.Net.BCrypt.HashPassword("Client123!"),
            Phone        = "+7 900 000-00-03",
            IsActive     = true
        };
        db.Users.Add(client);

        await db.SaveChangesAsync();

        // TrainerProfile для тренера
        db.TrainerProfiles.Add(new TrainerProfile
        {
            UserId         = trainer.UserId,
            Specialization = "Силовые тренировки",
            Experience     = 5,
            Description    = "Сертифицированный персональный тренер. Специализация: набор массы, похудение, силовой спорт.",
            TrainerCode    = "TRAINER01"
        });

        // ClientProfile для клиента (привязан к тренеру)
        db.ClientProfiles.Add(new ClientProfile
        {
            UserId         = client.UserId,
            TrainerId      = trainer.UserId,
            Gender         = "Female",
            HeightCm       = 165,
            InitialWeightKg = 62,
            GoalWeight     = 55,
            ActivityLevel  = "Moderate",
            FitnessGoal    = "Похудение"
        });

        await db.SaveChangesAsync();
    }

    private static async Task SeedFoodAsync(AppDbContext db)
    {
        if (await db.FoodProducts.AnyAsync()) return;

        var products = new List<FoodProduct>
        {
            // Молочные
            new() { Name = "Молоко 2.5%",          Brand = "Простоквашино", CaloriesPer100g = 52,  ProteinPer100g = 2.8m,  FatPer100g = 2.5m,  CarbsPer100g = 4.7m },
            new() { Name = "Творог 5%",             Brand = "Савушкин",      CaloriesPer100g = 121, ProteinPer100g = 17.2m, FatPer100g = 5.0m,  CarbsPer100g = 1.8m },
            new() { Name = "Греческий йогурт 0%",   Brand = "Чобани",        CaloriesPer100g = 59,  ProteinPer100g = 10.0m, FatPer100g = 0.4m,  CarbsPer100g = 3.6m },
            new() { Name = "Кефир 1%",              Brand = "Простоквашино", CaloriesPer100g = 40,  ProteinPer100g = 3.3m,  FatPer100g = 1.0m,  CarbsPer100g = 3.8m },
            new() { Name = "Сыр Российский",        Brand = null,            CaloriesPer100g = 364, ProteinPer100g = 23.0m, FatPer100g = 30.0m, CarbsPer100g = 0.0m },

            // Мясо и рыба
            new() { Name = "Куриная грудка",        Brand = null,            CaloriesPer100g = 113, ProteinPer100g = 23.6m, FatPer100g = 1.9m,  CarbsPer100g = 0.0m },
            new() { Name = "Говядина варёная",      Brand = null,            CaloriesPer100g = 254, ProteinPer100g = 25.8m, FatPer100g = 16.8m, CarbsPer100g = 0.0m },
            new() { Name = "Лосось",                Brand = null,            CaloriesPer100g = 208, ProteinPer100g = 20.0m, FatPer100g = 13.0m, CarbsPer100g = 0.0m },
            new() { Name = "Тунец в собственном соку", Brand = "Bonduelle",  CaloriesPer100g = 96,  ProteinPer100g = 22.0m, FatPer100g = 0.7m,  CarbsPer100g = 0.0m },
            new() { Name = "Яйцо куриное",          Brand = null,            CaloriesPer100g = 157, ProteinPer100g = 12.7m, FatPer100g = 11.5m, CarbsPer100g = 0.7m },
            new() { Name = "Индейка филе",          Brand = null,            CaloriesPer100g = 84,  ProteinPer100g = 19.2m, FatPer100g = 0.7m,  CarbsPer100g = 0.0m },

            // Крупы и злаки
            new() { Name = "Овсянка",               Brand = "Геркулес",      CaloriesPer100g = 352, ProteinPer100g = 12.3m, FatPer100g = 6.2m,  CarbsPer100g = 59.5m },
            new() { Name = "Гречка варёная",        Brand = null,            CaloriesPer100g = 92,  ProteinPer100g = 3.4m,  FatPer100g = 0.6m,  CarbsPer100g = 19.0m },
            new() { Name = "Рис белый варёный",     Brand = null,            CaloriesPer100g = 130, ProteinPer100g = 2.7m,  FatPer100g = 0.3m,  CarbsPer100g = 28.2m },
            new() { Name = "Макароны варёные",      Brand = "Barilla",       CaloriesPer100g = 158, ProteinPer100g = 5.5m,  FatPer100g = 0.9m,  CarbsPer100g = 30.9m },
            new() { Name = "Перловая крупа",        Brand = null,            CaloriesPer100g = 320, ProteinPer100g = 9.3m,  FatPer100g = 1.1m,  CarbsPer100g = 66.9m },

            // Бобовые
            new() { Name = "Чечевица варёная",      Brand = null,            CaloriesPer100g = 116, ProteinPer100g = 9.0m,  FatPer100g = 0.4m,  CarbsPer100g = 20.1m },
            new() { Name = "Фасоль красная варёная",Brand = null,            CaloriesPer100g = 127, ProteinPer100g = 8.4m,  FatPer100g = 0.5m,  CarbsPer100g = 22.8m },
            new() { Name = "Нут варёный",           Brand = null,            CaloriesPer100g = 164, ProteinPer100g = 8.9m,  FatPer100g = 2.6m,  CarbsPer100g = 27.4m },

            // Овощи
            new() { Name = "Брокколи",              Brand = null,            CaloriesPer100g = 34,  ProteinPer100g = 2.8m,  FatPer100g = 0.4m,  CarbsPer100g = 6.6m },
            new() { Name = "Огурец",                Brand = null,            CaloriesPer100g = 15,  ProteinPer100g = 0.8m,  FatPer100g = 0.1m,  CarbsPer100g = 3.0m },
            new() { Name = "Помидор",               Brand = null,            CaloriesPer100g = 18,  ProteinPer100g = 0.9m,  FatPer100g = 0.2m,  CarbsPer100g = 3.9m },
            new() { Name = "Капуста белокочанная",  Brand = null,            CaloriesPer100g = 27,  ProteinPer100g = 1.8m,  FatPer100g = 0.1m,  CarbsPer100g = 5.4m },
            new() { Name = "Картофель варёный",     Brand = null,            CaloriesPer100g = 80,  ProteinPer100g = 2.0m,  FatPer100g = 0.1m,  CarbsPer100g = 17.0m },
            new() { Name = "Морковь",               Brand = null,            CaloriesPer100g = 41,  ProteinPer100g = 0.9m,  FatPer100g = 0.2m,  CarbsPer100g = 9.6m },

            // Фрукты
            new() { Name = "Банан",                 Brand = null,            CaloriesPer100g = 89,  ProteinPer100g = 1.1m,  FatPer100g = 0.3m,  CarbsPer100g = 22.8m },
            new() { Name = "Яблоко",                Brand = null,            CaloriesPer100g = 52,  ProteinPer100g = 0.3m,  FatPer100g = 0.2m,  CarbsPer100g = 13.8m },
            new() { Name = "Апельсин",              Brand = null,            CaloriesPer100g = 47,  ProteinPer100g = 0.9m,  FatPer100g = 0.1m,  CarbsPer100g = 11.8m },
            new() { Name = "Виноград",              Brand = null,            CaloriesPer100g = 67,  ProteinPer100g = 0.6m,  FatPer100g = 0.2m,  CarbsPer100g = 17.2m },

            // Орехи
            new() { Name = "Миндаль",               Brand = null,            CaloriesPer100g = 579, ProteinPer100g = 21.2m, FatPer100g = 49.9m, CarbsPer100g = 21.7m },
            new() { Name = "Грецкий орех",          Brand = null,            CaloriesPer100g = 654, ProteinPer100g = 15.2m, FatPer100g = 65.2m, CarbsPer100g = 13.7m },
            new() { Name = "Арахисовая паста",      Brand = "JIF",           CaloriesPer100g = 588, ProteinPer100g = 25.0m, FatPer100g = 50.4m, CarbsPer100g = 20.1m },

            // Спортивное питание
            new() { Name = "Протеин сывороточный",  Brand = "Optimum Nutrition", CaloriesPer100g = 370, ProteinPer100g = 80.0m, FatPer100g = 4.0m, CarbsPer100g = 6.0m },
            new() { Name = "Батончик протеиновый",  Brand = "Quest",         CaloriesPer100g = 367, ProteinPer100g = 42.9m, FatPer100g = 14.3m, CarbsPer100g = 44.9m, FiberPer100g = 28.6m },

            // Масла
            new() { Name = "Масло оливковое",       Brand = null,            CaloriesPer100g = 884, ProteinPer100g = 0.0m,  FatPer100g = 100.0m, CarbsPer100g = 0.0m },
            new() { Name = "Масло сливочное",       Brand = null,            CaloriesPer100g = 717, ProteinPer100g = 0.5m,  FatPer100g = 81.1m, CarbsPer100g = 0.1m },

            // Хлеб
            new() { Name = "Хлеб ржаной",           Brand = null,            CaloriesPer100g = 259, ProteinPer100g = 8.5m,  FatPer100g = 3.3m,  CarbsPer100g = 48.3m },
            new() { Name = "Хлебцы рисовые",        Brand = "Dr. Korner",    CaloriesPer100g = 382, ProteinPer100g = 8.4m,  FatPer100g = 2.9m,  CarbsPer100g = 81.5m },
        };

        db.FoodProducts.AddRange(products);
        await db.SaveChangesAsync();
    }
}
