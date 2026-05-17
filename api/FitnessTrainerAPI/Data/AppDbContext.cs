using FitnessTrainerAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FitnessTrainerAPI.Data;

public class AppDbContext(DbContextOptions<AppDbContext> options) : DbContext(options)
{
    public DbSet<EmailVerificationCode> EmailVerificationCodes => Set<EmailVerificationCode>();
    public DbSet<Role>            Roles            => Set<Role>();
    public DbSet<User>            Users            => Set<User>();
    public DbSet<RefreshToken>    RefreshTokens    => Set<RefreshToken>();
    public DbSet<TrainerProfile>  TrainerProfiles  => Set<TrainerProfile>();
    public DbSet<ClientProfile>   ClientProfiles   => Set<ClientProfile>();
    public DbSet<Measurement>     Measurements     => Set<Measurement>();
    public DbSet<ProgressPhoto>   ProgressPhotos   => Set<ProgressPhoto>();
    public DbSet<FoodProduct>     FoodProducts     => Set<FoodProduct>();
    public DbSet<FoodDiaryEntry>  FoodDiary        => Set<FoodDiaryEntry>();
    public DbSet<WorkoutPlan>     WorkoutPlans     => Set<WorkoutPlan>();
    public DbSet<WorkoutDay>      WorkoutDays      => Set<WorkoutDay>();
    public DbSet<Exercise>        Exercises        => Set<Exercise>();
    public DbSet<WorkoutLog>      WorkoutLogs      => Set<WorkoutLog>();
    public DbSet<ExerciseLog>     ExerciseLogs     => Set<ExerciseLog>();
    public DbSet<Message>         Messages         => Set<Message>();
    public DbSet<Notification>    Notifications    => Set<Notification>();
    public DbSet<FcmToken>        FcmTokens        => Set<FcmToken>();
    public DbSet<TrainerReview>   TrainerReviews   => Set<TrainerReview>();

    protected override void OnModelCreating(ModelBuilder mb)
    {
        // Role
        mb.Entity<Role>().ToTable("Roles");
        mb.Entity<Role>().HasData(
            new Role { RoleId = 1, RoleName = "Trainer" },
            new Role { RoleId = 2, RoleName = "Client" },
            new Role { RoleId = 3, RoleName = "Admin" }
        );

        // User
        mb.Entity<User>().ToTable("Users");
        mb.Entity<User>()
            .HasOne(u => u.Role)
            .WithMany(r => r.Users)
            .HasForeignKey(u => u.RoleId);

        // Messages — два FK на одну таблицу Users
        mb.Entity<Message>().ToTable("Messages");
        mb.Entity<Message>()
            .HasOne(m => m.Sender)
            .WithMany(u => u.SentMessages)
            .HasForeignKey(m => m.SenderId)
            .OnDelete(DeleteBehavior.Restrict);
        mb.Entity<Message>()
            .HasOne(m => m.Receiver)
            .WithMany(u => u.ReceivedMessages)
            .HasForeignKey(m => m.ReceiverId)
            .OnDelete(DeleteBehavior.Restrict);

        // ClientProfile — два FK на Users
        mb.Entity<ClientProfile>().ToTable("ClientProfiles");
        mb.Entity<ClientProfile>().Property(cp => cp.Gender).HasMaxLength(10);
        mb.Entity<ClientProfile>().Property(cp => cp.HeightCm).HasPrecision(6, 2);
        mb.Entity<ClientProfile>().Property(cp => cp.InitialWeightKg).HasPrecision(6, 2);
        mb.Entity<ClientProfile>().Property(cp => cp.GoalWeight).HasPrecision(6, 2);
        mb.Entity<ClientProfile>().Property(cp => cp.DailyCalorieGoal).HasPrecision(8, 2);
        mb.Entity<ClientProfile>().Property(cp => cp.DailyProteinGoal).HasPrecision(6, 2);
        mb.Entity<ClientProfile>().Property(cp => cp.DailyFatGoal).HasPrecision(6, 2);
        mb.Entity<ClientProfile>().Property(cp => cp.DailyCarbGoal).HasPrecision(6, 2);
        mb.Entity<ClientProfile>()
            .HasOne(cp => cp.User)
            .WithOne(u => u.ClientProfile)
            .HasForeignKey<ClientProfile>(cp => cp.UserId)
            .OnDelete(DeleteBehavior.Cascade);
        mb.Entity<ClientProfile>()
            .HasOne(cp => cp.Trainer)
            .WithMany()
            .HasForeignKey(cp => cp.TrainerId)
            .OnDelete(DeleteBehavior.Restrict);

        // WorkoutPlan — два FK на Users
        mb.Entity<WorkoutPlan>().ToTable("WorkoutPlans").HasKey(e => e.PlanId);
        mb.Entity<WorkoutPlan>()
            .HasOne(wp => wp.Trainer)
            .WithMany()
            .HasForeignKey(wp => wp.TrainerId)
            .OnDelete(DeleteBehavior.Restrict);
        mb.Entity<WorkoutPlan>()
            .HasOne(wp => wp.Client)
            .WithMany()
            .HasForeignKey(wp => wp.ClientId)
            .OnDelete(DeleteBehavior.Restrict);

        // Computed column BMI — только для чтения
        mb.Entity<Measurement>().ToTable("Measurements");
        mb.Entity<Measurement>()
            .Property(m => m.BMI)
            .ValueGeneratedOnAddOrUpdate();
        mb.Entity<Measurement>().Property(m => m.WeightKg).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.HeightCm).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.ChestCm).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.WaistCm).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.HipsCm).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.NeckCm).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.BicepCm).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.ForearmCm).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.ThighCm).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.CalfCm).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.BodyFatPercent).HasPrecision(5, 2);
        mb.Entity<Measurement>().Property(m => m.MuscleMassKg).HasPrecision(6, 2);
        mb.Entity<Measurement>().Property(m => m.BMI).HasPrecision(5, 2);

        mb.Entity<FoodProduct>().ToTable("FoodProducts").HasKey(e => e.ProductId);
        mb.Entity<FoodProduct>()
            .HasOne(p => p.Creator)
            .WithMany()
            .HasForeignKey(p => p.CreatedBy)
            .OnDelete(DeleteBehavior.SetNull);
        mb.Entity<FoodProduct>().Property(p => p.CaloriesPer100g).HasPrecision(8, 2);
        mb.Entity<FoodProduct>().Property(p => p.ProteinPer100g).HasPrecision(6, 2);
        mb.Entity<FoodProduct>().Property(p => p.FatPer100g).HasPrecision(6, 2);
        mb.Entity<FoodProduct>().Property(p => p.CarbsPer100g).HasPrecision(6, 2);
        mb.Entity<FoodProduct>().Property(p => p.FiberPer100g).HasPrecision(6, 2);

        mb.Entity<FoodDiaryEntry>().ToTable("FoodDiary").HasKey(e => e.DiaryId);
        mb.Entity<FoodDiaryEntry>().Property(e => e.WeightG).HasPrecision(8, 2);
        mb.Entity<TrainerProfile>().ToTable("TrainerProfiles");
        mb.Entity<TrainerProfile>().HasIndex(tp => tp.TrainerCode).IsUnique();
        mb.Entity<RefreshToken>().ToTable("RefreshTokens").HasKey(e => e.TokenId);
        mb.Entity<ProgressPhoto>().ToTable("ProgressPhotos").HasKey(e => e.PhotoId);
        mb.Entity<WorkoutDay>().ToTable("WorkoutDays").HasKey(e => e.DayId);
        mb.Entity<Exercise>().ToTable("Exercises");
        mb.Entity<Exercise>().Property(e => e.WeightKg).HasPrecision(6, 2);
        mb.Entity<WorkoutLog>().ToTable("WorkoutLogs").HasKey(e => e.LogId);
        mb.Entity<ExerciseLog>().ToTable("ExerciseLogs");
        mb.Entity<ExerciseLog>().Property(e => e.WeightActualKg).HasPrecision(6, 2);
        mb.Entity<Notification>().ToTable("Notifications");
        mb.Entity<FcmToken>().ToTable("FcmTokens").HasKey(e => e.TokenId);
        mb.Entity<EmailVerificationCode>().ToTable("EmailVerificationCodes");

        // TrainerReview — два FK на Users (Client + Trainer)
        mb.Entity<TrainerReview>().ToTable("TrainerReviews").HasKey(e => e.ReviewId);
        mb.Entity<TrainerReview>()
            .HasOne(r => r.Client)
            .WithMany()
            .HasForeignKey(r => r.ClientId)
            .OnDelete(DeleteBehavior.Restrict);
        mb.Entity<TrainerReview>()
            .HasOne(r => r.Trainer)
            .WithMany()
            .HasForeignKey(r => r.TrainerId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}
