using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace FitnessTrainerAPI.Migrations
{
    /// <inheritdoc />
    public partial class AddTrainerCode : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "TrainerCode",
                table: "TrainerProfiles",
                type: "nvarchar(450)",
                nullable: false,
                defaultValue: "");

            migrationBuilder.CreateIndex(
                name: "IX_TrainerProfiles_TrainerCode",
                table: "TrainerProfiles",
                column: "TrainerCode",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_TrainerProfiles_TrainerCode",
                table: "TrainerProfiles");

            migrationBuilder.DropColumn(
                name: "TrainerCode",
                table: "TrainerProfiles");
        }
    }
}
